require 'mysql2'
require 'redis'
require 'json'
require 'slim'

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

def get_redis
  Redis.new(:path => "/tmp/redis.sock")
end

def render_html(ticket_id)
  mysql = connection
  redis = get_redis
  @ticket = mysql.query(
    "SELECT t.*, a.name AS artist_name FROM ticket t
     INNER JOIN artist a ON t.artist_id = a.id
     WHERE t.id = #{ mysql.escape(ticket_id.to_s) } LIMIT 1",
  ).first
  @variations = mysql.query(
    "SELECT id, name FROM variation WHERE ticket_id = #{ mysql.escape(@ticket['id'].to_s) } ORDER BY id",
  )
  @variations.each do |variation|
    variation["count"] = redis.get("variation_remain_count_#{variation['id']}")
    variation["stock"] = {}
    mysql.query(
      "SELECT seat_id, order_id FROM stock
       WHERE variation_id = #{ mysql.escape(variation['id'].to_s) }",
    ).each do |stock|
      variation["stock"][stock["seat_id"]] = stock["order_id"]
    end
  end
  Slim::Template.new('views/seat.slim', {}).render(self)
end

def cache_seat_html(ticket_id)
  redis = get_redis
  html = render_html(ticket_id)
  p html.size
  redis.set("seat_cache_#{ticket_id}", html)
end

begin
  5.times do |i|
    Process.fork do
      loop do
        begin
          warn "render seat for #{i + 1}"
          cache_seat_html(i+1)
          sleep(0.5)
        rescue Interrupt => e
          exit
        rescue Exception => e
          warn e
        end
      end
    end
  end
rescue Interrupt => e
  exit
rescue Exception => e
  warn e
end

loop do
  sleep(1)
end
