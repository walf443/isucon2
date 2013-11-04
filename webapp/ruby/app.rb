require 'sinatra/base'
require 'slim'
require 'json'
require 'mysql2'
require 'redis'
require 'msgpack'

class Isucon2App < Sinatra::Base
  $stdout.sync = true
  set :slim, :pretty => true, :layout => true

  helpers do
    def connection
      config = JSON.parse(IO.read(File.dirname(__FILE__) + "/../config/common.#{ ENV['ISUCON_ENV'] || 'local' }.json"))['database']
      Mysql2::Client.new(
        :host => config['host'],
        :port => config['port'],
        :username => config['username'],
        :password => config['password'],
        :database => config['dbname'],
        :reconnect => true,
      )
    end

    def recent_sold
      redis = get_redis
      mysql = connection
      row_raws = redis.lrange("order_request", 0, 10)
      variation_ids = []
      return [] if row_raws.empty?
      
      rows = []
      row_raws.each do |raw_row|
        row = MessagePack.unpack(raw_row)
        variation_ids.push(row["variation_id"])
        rows.push(row)
      end
      query = ("SELECT variation.id as variation_id, variation.name as v_name, ticket.name as t_name, artist.name as a_name 
        FROM variation 
        JOIN ticket ON variation.ticket_id = ticket.id 
        JOIN artist on artist.id = ticket.artist_id  WHERE variation.id IN (#{variation_ids.join(',')})
      ")
      variations = mysql.query(query)
      variation_of = {}
      variations.each do |variation|
        variation_of[variation["variation_id"]] = variation
      end
      rows.each do |row|
        variation = variation_of[row["variation_id"]]
        ["v_name", "t_name", "a_name"].each do |col|
          row[col] = variation[col]
        end
      end
      return rows
    end

    def get_redis
      Redis.new
    end
  end

  # main

  get '/' do
    mysql = connection
    artists = mysql.query("SELECT * FROM artist ORDER BY id")
    slim :index, :locals => {
      :artists => artists,
    }
  end

  get '/artist/:artistid' do
    mysql = connection
    artist  = mysql.query(
      "SELECT id, name FROM artist WHERE id = #{ mysql.escape(params[:artistid]) } LIMIT 1",
    ).first
    tickets = mysql.query(
      "SELECT id, name FROM ticket WHERE artist_id = #{ mysql.escape(artist['id'].to_s) } ORDER BY id",
    )
    redis = get_redis
    tickets.each do |ticket|
      ticket["count"] = redis.get("ticket_remain_count_#{ticket['id']}")
    end
    slim :artist, :locals => {
      :artist  => artist,
      :tickets => tickets,
    }
  end

  get '/ticket/:ticketid' do
    mysql = connection
    redis = get_redis
    ticket = mysql.query(
      "SELECT t.*, a.name AS artist_name FROM ticket t
       INNER JOIN artist a ON t.artist_id = a.id
       WHERE t.id = #{ mysql.escape(params[:ticketid]) } LIMIT 1",
    ).first
    variations = mysql.query(
      "SELECT id, name FROM variation WHERE ticket_id = #{ mysql.escape(ticket['id'].to_s) } ORDER BY id",
    )
    variations.each do |variation|
      variation["count"] = redis.get("variation_remain_count_#{variation['id']}")
    end
    seat_html = redis.get("seat_cache_#{params[:ticketid]}")
    slim :ticket, :locals => {
      :ticket     => ticket,
      :variations => variations,
      :seat_html => seat_html
    }
  end

  post '/buy' do
    mysql = connection
    mysql.query('BEGIN')
    mysql.query("INSERT INTO order_request (member_id) VALUES ('#{ mysql.escape(params[:member_id]) }')")
    order_id = mysql.last_id
    mysql.query(
      "UPDATE stock SET order_id = #{ mysql.escape(order_id.to_s) }
       WHERE variation_id = #{ mysql.escape(params[:variation_id]) } AND order_id IS NULL
       ORDER BY id DESC LIMIT 1",
    )
    if mysql.affected_rows > 0
      redis = get_redis
      stock = mysql.query(
        "SELECT* from stock join variation on (variation.id = stock.variation_id) where order_id = #{ mysql.escape(order_id.to_s) } LIMIT 1",
      ).first
      redis.pipelined do
        redis.lpush("order_request", { "order_id" =>  order_id, "stock_id" => stock["id"], "variation_id" => stock["variation_id"], "seat_id" => stock["seat_id"] }.to_msgpack)
        redis.decr("ticket_remain_count_#{stock['ticket_id']}")
        redis.decr("variation_remain_count_#{stock['variation_id']}")
      end
      mysql.query('COMMIT')
      slim :complete, :locals => { :seat_id => stock["seat_id"], :member_id => params[:member_id] }
    else
      mysql.query('ROLLBACK')
      slim :soldout
    end
  end

  # admin

  get '/admin' do
    slim :admin
  end

  get '/admin/order.csv' do
    mysql = connection
    body  = ''
    orders = mysql.query(
      'SELECT order_request.*, stock.seat_id, stock.variation_id, stock.updated_at
       FROM order_request JOIN stock ON order_request.id = stock.order_id
       ORDER BY order_request.id ASC',
    )
    orders.each do |order|
      order['updated_at'] = order['updated_at'].strftime('%Y-%m-%d %X')
      body += order.values_at('id', 'member_id', 'seat_id', 'variation_id', 'updated_at').join(',')
      body += "\n"
    end
    [200, { 'Content-Type' => 'text/csv' }, body]
  end

  post '/admin' do
    redis = get_redis
    mysql = connection
    redis.flushall
    open(File.dirname(__FILE__) + '/../config/database/initial_data.sql') do |file|
      file.each do |line|
        next unless line.strip!.length > 0
        mysql.query(line)
      end
    end
    ticket_remain_counts = mysql.query("select ticket_id, count(*) as count from stock join variation on (stock.variation_id = variation.id) group by ticket_id")
    ticket_remain_counts.each do |row|
      redis.set("ticket_remain_count_#{row["ticket_id"]}", row["count"])
    end
    variation_remain_counts = mysql.query("select variation_id, count(*) as count from stock  group by variation_id")
    variation_remain_counts.each do |row|
      redis.set("variation_remain_count_#{row["variation_id"]}", row["count"])
    end
    redirect '/admin', 302
  end

  run! if app_file == $0
end
