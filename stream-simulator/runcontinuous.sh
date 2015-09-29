while true; do
  nohup java -Xmx1024m -jar /root/sedev/demo-artifacts/storm_demo_2.2/storm_demo/stream-simulator/target/stream-simulator.jar 6 250 com.hortonworks.streaming.impl.domain.transport.Truck com.hortonworks.streaming.impl.collectors.KafkaEventCollector 1000 /etc/storm_demo/routes/midwest 1000 &
  last_pid=$!
  sleep 240
  echo "PID $last_pid"
  kill -9 $last_pid
  truncate nohup.out --size 0
done
