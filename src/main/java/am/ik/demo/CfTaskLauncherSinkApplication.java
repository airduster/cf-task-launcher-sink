package am.ik.demo;

import java.nio.file.Paths;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.task.launcher.TaskLaunchRequest;
import org.springframework.core.io.Resource;
import org.springframework.messaging.Message;

@SpringBootApplication
@EnableBinding(Sink.class)
public class CfTaskLauncherSinkApplication {

	private final TaskLauncher taskLauncher;
	private final DelegatingResourceLoader resourceLoader;
	private final String awsAccessKey;
	private final String awsSecretKey;

	public CfTaskLauncherSinkApplication(TaskLauncher taskLauncher,
			DelegatingResourceLoader resourceLoader,
			@Value("${AWS_ACCESS_KEY}") String awsAccessKey,
			@Value("${AWS_SECRET_KEY}") String awsSecretKey) {
		this.taskLauncher = taskLauncher;
		this.resourceLoader = resourceLoader;
		this.awsAccessKey = awsAccessKey;
		this.awsSecretKey = awsSecretKey;
	}

	@StreamListener(Sink.INPUT)
	public void deploy(Message<String> message) {
		System.out.println(message);
		String fileName = Paths.get(message.getPayload()).getFileName().toString();
		System.out.println(fileName);
		TaskLaunchRequest taskLaunchRequest = new TaskLaunchRequest(
				"https://github.com/making-demo-scdf/index-import-task/releases/download/1.0.0-dev0/index-import-task-1.0.0-dev0.jar",
				Arrays.asList("fileName=" + fileName, "--AWS_ACCESS_KEY=" + awsAccessKey,
						"--AWS_SECRET_KEY=" + awsSecretKey),
				null, null);
		System.out.println(taskLaunchRequest);
		Resource resource = resourceLoader.getResource(taskLaunchRequest.getUri());
		AppDefinition definition = new AppDefinition(
				"Task-" + taskLaunchRequest.hashCode(),
				taskLaunchRequest.getEnvironmentProperties());
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource,
				taskLaunchRequest.getDeploymentProperties(),
				taskLaunchRequest.getCommandlineArguments());
		this.taskLauncher.launch(request);
	}

	public static void main(String[] args) {
		SpringApplication.run(CfTaskLauncherSinkApplication.class, args);
	}
}
