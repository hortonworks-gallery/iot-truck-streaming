while true; do
  nohup java -Xmx1024m -jar target/stream-simulator.jar 6 250 com.hortonworks.streaming.impl.domain.transport.Truck com.hortonworks.streaming.impl.collectors.StdOutEventCollector 1000 /etc/storm_demo/routes/midwest 1000 &
  last_pid=$!
  sleep 240
  echo "PID $last_pid"
  kill -9 $last_pid
  truncate nohup.out --size 0
done
