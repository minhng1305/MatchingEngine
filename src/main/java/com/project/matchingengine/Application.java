package com.project.matchingengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	// @Bean
	// public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
	// 	return args -> {

	// 		System.out.println("Let's inspect the beans provided by Spring Boot:");

	// 		String[] beanNames = ctx.getBeanDefinitionNames();
	// 		Arrays.sort(beanNames);
	// 		for (String beanName : beanNames) {
	// 			System.out.println(beanName);
	// 		}

	// 	};
	// }

    // @KafkaListener(id = "myId", topics = "topic1")
    // public void listen(String in) {
    //     System.out.println(in);
    // }

	// @Bean
	// public KafkaAdmin admin() {
	// 	Map<String, Object> configs = new HashMap<>();
	// 	configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
	// 	return new KafkaAdmin(configs);
	// }

	// @Bean
	// public NewTopic topic1() {
	// 	return TopicBuilder.name("thing1")
	// 			.partitions(10)
	// 			.replicas(3)
	// 			.compact()
	// 			.build();
	// }

	// @Bean
	// public NewTopic topic2() {
	// 	return TopicBuilder.name("thing2")
	// 			.partitions(10)
	// 			.replicas(3)
	// 			.config(TopicConfig.COMPRESSION_TYPE_CONFIG, "zstd")
	// 			.build();
	// }

	// @Bean
	// public NewTopic topic3() {
	// 	return TopicBuilder.name("thing3")
	// 			.assignReplicas(0, List.of(0, 1))
	// 			.assignReplicas(1, List.of(1, 2))
	// 			.assignReplicas(2, List.of(2, 0))
	// 			.config(TopicConfig.COMPRESSION_TYPE_CONFIG, "zstd")
	// 			.build();
	// }


}