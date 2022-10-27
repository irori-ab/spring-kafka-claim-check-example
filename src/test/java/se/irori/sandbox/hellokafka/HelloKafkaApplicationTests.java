package se.irori.sandbox.hellokafka;

import static se.irori.kafka.claimcheck.azure.AzureClaimCheckConfig.Keys.AZURE_STORAGE_ACCOUNT_CONNECTION_STRING_CONFIG;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
class HelloKafkaApplicationTests {
	@Container
	public static final AzuriteContainer azuriteContainer = new AzuriteContainer()
			.withExposedPorts(10000);

	// https://docs.confluent.io/platform/current/installation/versions-interoperability.html
	// 7.1.x => Kafka 3.1.0
	@Container
	public static final KafkaContainer kafkaContainer =
			new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.1.1"));


	@Test
	void contextLoads() {
	}

	@DynamicPropertySource
	static void dynamicProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.kafka.bootstrap-servers", () -> Collections.singletonList(kafkaContainer.getBootstrapServers()));

		registry.add("spring.kafka.properties." + AZURE_STORAGE_ACCOUNT_CONNECTION_STRING_CONFIG,
				() -> getAzuriteConfig());
	}

	static String getAzuriteConfig() {
		String token =
				"Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
		String host = "" + azuriteContainer.getHost();
		String account = "devstoreaccount1";
		// http://<local-machine-address>:<port>/<account-name>/<resource-path>
		String endpoint = String.format("http://%s:%d/%s/",
				host,
				azuriteContainer.getMappedPort(10000),
				account);
		String connectionString = "" +
				"DefaultEndpointsProtocol=http;" +
				"AccountName=devstoreaccount1;" +
				"BlobEndpoint=" + endpoint + ";" +
				"AccountKey=" + token + ";";

		return connectionString;
	}

}
