package uk.ac.ebi.literature.textminingapi.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.ac.ebi.literature.textminingapi.pojo.Components;
import uk.ac.ebi.literature.textminingapi.pojo.MLTextObject;
import uk.ac.ebi.literature.textminingapi.service.ExecuteDictionaryService;
import uk.ac.ebi.literature.textminingapi.service.FileService;
import uk.ac.ebi.literature.textminingapi.service.MLQueueSenderService;
import uk.ac.ebi.literature.textminingapi.utility.Utility;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@ConditionalOnProperty(name = "rabbitmq.enableListening", havingValue = "true")
public class MlFilterListener {

	Logger logger = LogManager.getLogger(MlFilterListener.class);

	@Value("${raw.annotation.filename}")
	private String RAW_ANNOTATION_FILENAME;

	private final ExecuteDictionaryService executeDictionaryService;

	private final MLQueueSenderService queueSenderService;

	@Value("${rabbitmq.tmExchange}")
	private String TM_EXCHANGE;

	@Value("${rabbitmq.outcomeQueue}")
	private String QUEUE_OUTCOME_MESSAGES;

	@Value("${rabbitmq.rawAnnotationQueue}")
	private String QUEUE_RAWANNOTATION_MESSAGES;

	private final FileService fileService;

	public MlFilterListener(ExecuteDictionaryService executeDictionaryService, MLQueueSenderService queueSenderService, FileService fileService) {
		this.executeDictionaryService = executeDictionaryService;
		this.queueSenderService = queueSenderService;
		this.fileService = fileService;
	}

	@RabbitListener(id = "plaintext-queue-listener", autoStartup = "${rabbitmq.plaintextQueue.autoStartUp}", queues = "${rabbitmq.plaintextQueue}")
	public void listenForPlainText(Message in) throws Exception {
		MLTextObject plainTextMessage = Utility.castMessage(in, MLTextObject.class);

		if (queueSenderService.hasExceededRetryCount(in)) {
			this.sendErrorToOutcomeQueue(plainTextMessage,
					Collections.singletonList("Impossible to textmine the file " + plainTextMessage.getFilename()));
			boolean deleted = fileService.delete(plainTextMessage.getProcessingFilename());
			logger.info("{" + plainTextMessage.getFtId() + "," + plainTextMessage.getUser() + "} Deleted "
					+ plainTextMessage.getProcessingFilename() + "? " + deleted);
		} else {
			this.performAction(plainTextMessage);

		}
	}

	private void performAction(MLTextObject plainTextMessage) throws Exception {

		final String inputFileName = plainTextMessage.getProcessingFilename();
		logger.info("started Processing Id: "+ plainTextMessage.getFtId() + " with file name: "+ inputFileName);
		final String outFileName = plainTextMessage.getUser() + "_" +
				plainTextMessage.getFtId() + "_" + plainTextMessage.getFilename() +
				RAW_ANNOTATION_FILENAME;

		executeDictionaryService.executeAllDictionaries(inputFileName, outFileName, plainTextMessage.getFtId());
		this.sendMessageToRawAnnotationsQueue(plainTextMessage, outFileName);

		fileService.delete(inputFileName);
		logger.info("finished processing Id: "+ plainTextMessage.getFtId() + " with file name: "+ inputFileName);
	}

	private void sendErrorToOutcomeQueue(MLTextObject message, List<String> errors) throws Exception {
		logger.info("sending data to tm_outcome for fileName: " + message.getFilename());
		StringBuilder errorString = new StringBuilder();
		errors.stream().forEach(e -> errorString.append(e).append(" "));
		Utility.markMessageAsFailed(message, Components.ANNOTATOR, errorString.toString());
		boolean flag = queueSenderService.sendMessageToQueue(QUEUE_OUTCOME_MESSAGES, message, TM_EXCHANGE);
		if (!flag) {
			logger.error(
					"sending data to outcome queue failed with following errors: Network errors prevented messages to be processed successfully");
			throw new Exception(
					"sending data to outcome queue failed with following errors: Network errors prevented messages to be processed successfully");
		}
		logger.info("data sent to outcome queue successfully: " + message.getFilename());
	}

	private void sendMessageToRawAnnotationsQueue(MLTextObject message, String outputFileName) throws Exception {

		logger.info("sending data to raw_annotation for fileName: " + message.getFilename());

		message.setProcessingFilename(outputFileName);
		message.setError(null);
		message.setErrorComponent(null);

		boolean flag = queueSenderService.sendMessageToQueue(QUEUE_RAWANNOTATION_MESSAGES, message, TM_EXCHANGE);
		if (!flag) {
			logger.error(
					"sending data to raw_annotation queue failed with following errors: Network errors prevented messages to be processed successfully");
			throw new Exception("Impossible to store Success in raw_annotation queue  for " + message.toString());
		}
		logger.info("data sent to raw_annotation queue successfully: " + message.getFilename());

	}

}
