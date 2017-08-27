package com.kafka.demo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;

/* https://cwiki.apache.org/confluence/display/KAFKA/Consumer+Group+Example
 * http://kafka.apache.org/documentation.html#consumerapi
 */
public class ConsumerDemo {
	private final ConsumerConnector consumer;
	private final String topic;
	private ExecutorService executor;

	public ConsumerDemo(String a_zookeeper, String a_groupId, String a_topic) {
		this.consumer = kafka.consumer.Consumer.createJavaConsumerConnector(createConsumerConfig(a_zookeeper,a_groupId));
		this.topic = a_topic;
	}

	public void shutdown() {
		if (consumer != null)
			consumer.shutdown();
		if (executor != null)
			executor.shutdown();
		try {
            if (!executor.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                System.out.println("Timed out waiting for consumer threads to shut down, exiting uncleanly");
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupted during shutdown, exiting uncleanly");
        }
	}

	public void run(int numThreads) {
		System.out.println("-----Consumers begin to execute-------");
		Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
		topicCountMap.put(topic, new Integer(numThreads));
		Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer
				.createMessageStreams(topicCountMap);
		
		/*KafkaStream<byte[], byte[]> stream2 = consumerMap.get(topic).get(0);
		ConsumerIterator<byte[], byte[]> it = stream2.iterator();
		System.out.println("*********Results********");
		while (true) {
			if (it.hasNext()) {
				// 打印得到的消息
				System.err.println(Thread.currentThread() + " get data:" + new String(it.next().message()));
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}*/
		
		List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topic);
		System.err.println("-----Need to consume content----"+streams);

		// now launch all the threads
		executor = Executors.newFixedThreadPool(numThreads);

		// now create an object to consume the messages
		int threadNumber = 0;
		for (final KafkaStream<byte[], byte[]> stream : streams) {
			System.out.println("-----Consumers begin to consume-------"+stream);
			executor.submit(new ConsumerMsgTask(stream, threadNumber));
			threadNumber++;
		}
	}

	private static ConsumerConfig createConsumerConfig(String a_zookeeper,
			String a_groupId) {
		Properties props = new Properties();
		// see http://kafka.apache.org/08/configuration.html --3.2 Consumer Configs
		// http://kafka.apache.org/documentation.html#consumerconfigs
		props.put("zookeeper.connect", a_zookeeper); //配置ZK地址
		props.put("group.id", a_groupId); //必填字段
		props.put("zookeeper.session.timeout.ms", "4000");
		props.put("zookeeper.sync.time.ms", "200");
		props.put("auto.commit.interval.ms", "1000");
		props.put("auto.offset.reset", "smallest");
		props.put("serializer.class", "kafka.serializer.StringEncoder ");
		return new ConsumerConfig(props);
	}

	public static void main(String[] arg) {
		String[] args = { "127.0.0.1:12181", "tom2", "page_visits", "1" };
		String zooKeeper = args[0];
		String groupId = args[1];
		String topic = args[2];
		int threads = Integer.parseInt(args[3]);

		ConsumerDemo demo = new ConsumerDemo(zooKeeper, groupId, topic);
		demo.run(threads);

		try {
			Thread.sleep(100000);
		} catch (InterruptedException ie) {

		}
		demo.shutdown();
	}
}
