package utwente.hmi.woolservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import eu.woolplatform.utils.exception.DatabaseException;
import eu.woolplatform.webservice.dialogue.UserService;
import eu.woolplatform.wool.exception.WoolException;
import eu.woolplatform.wool.execution.ExecuteNodeResult;
import eu.woolplatform.wool.model.DialogueState;
import eu.woolplatform.wool.model.WoolNode;
import eu.woolplatform.wool.model.protocol.DialogueMessage;
import eu.woolplatform.wool.model.protocol.DialogueMessageFactory;

public class JSONDataController {
	
	
	
	public class WoolAPIException extends Exception { 
		private static final long serialVersionUID = 1L;

		public WoolAPIException(String errorMessage) {
	        super(errorMessage);
	    }
	}
	
	WoolApplication application;
	ObjectMapper objectMapper;
	
	public JSONDataController(WoolApplication application) {
		this.application = application;
		this.objectMapper = new ObjectMapper();
	}
	
	public JsonNode handleRequest(JsonNode jn) throws WoolAPIException, IOException, DatabaseException, WoolException, IllegalArgumentException, ClassNotFoundException {
		if (!jn.has("request") || !jn.has("body"))
			return null;

		JsonNode response = null;
		String request = jn.get("request").asText();
		JsonNode body = jn.get("body");
		switch(request) {
			case "getVariables":
				response = getVariables(body);
				break;
			case "setVariables":
				response = setVariables(body);
				break;
			case "startDialogue":
				response = startDialogue(body);
				break;
			case "currentDialogue":
				response = currentDialogue(body);
				break;
			case "progressDialogue":
				response = progressDialogue(body);
				break;
			case "backDialogue":
				response = backDialogue(body);
				break;
			case "cancelDialogue":
				response = cancelDialogue(body);
				break;
		}
		
		return response;
	}
	
	// GET /variables 
	public JsonNode getVariables(JsonNode jn) throws WoolAPIException, IOException, DatabaseException {
		// String user, String names
		String userId = jn.get("userId").asText().trim(); // force json array?
		String names = jn.get("names").asText().trim(); // force json array?
		UserService userService = application.getServiceManager().getActiveUserService(userId);
		Map<String,?> varStore = userService.variableStore.getModifiableMap(false, null);
		List<String> nameList;
		if (names.length() == 0) {
			nameList = new ArrayList<>(varStore.keySet());
			Collections.sort(nameList);
		} else {
			List<String> invalidNames = new ArrayList<>();
			String[] nameArray = names.split("\\s+");
			for (String name : nameArray) {
				if (!name.matches("[A-Za-z][A-Za-z0-9_]*"))
					invalidNames.add(name);
			}
			if (!invalidNames.isEmpty()) {
				throw new WoolAPIException("Invalid variable names: " + String.join(", ", invalidNames));
			}
			nameList = Arrays.asList(nameArray);
		}
		//Map<String, Object> result = new LinkedHashMap<>();
		ObjectNode res = objectMapper.createObjectNode();
		for (String name : nameList) {
			Object o = varStore.get(name);
			if (o.getClass() == Integer.class) {
				res.put(name, (int) o);
			} else if (o.getClass() == String.class) {
				res.put(name, (String) o);
			} else if (o.getClass() == Boolean.class) {
				res.put(name, (boolean) o);
			} else if (o.getClass() == Float.class) {
				res.put(name, (float) o);
			} else if (o.getClass() == Double.class) {
				res.put(name, (double) o);
			} else {
				throw new WoolAPIException("Cannot convert varaible type from store: "+name+" ("+o.getClass().toString()+")");
			}
		}
		return (JsonNode) res;
	}

	// POST /variables
	public JsonNode setVariables(JsonNode jn) throws WoolAPIException, DatabaseException, IOException {
		String userId = jn.get("userId").asText();
		Map<String, String> variables = new HashMap<String, String>();
		//System.out.println(jn.toPrettyString());
		
		JsonNode varNode = jn.get("variables");
		//System.out.println(varNode.toPrettyString());

		if (varNode.isArray()) {
			for (JsonNode variableEntry : varNode) {
				String vname = variableEntry.get("name").asText();
				JsonNodeType type = variableEntry.get("value").getNodeType();
				
				if (type == JsonNodeType.NUMBER) {
					//variables.put(vname, variableEntry.get("value").asDouble());
				} else if (type == JsonNodeType.STRING) {
					variables.put(vname, variableEntry.get("value").asText());
				} else if (type == JsonNodeType.BOOLEAN) {
					//variables.put(vname, variableEntry.get("value").asBoolean());
				} else {
					throw new WoolAPIException("Cannot convert varaible type from store: "+vname+" ("+variableEntry.get("value").asText()+")");
				}
			}
		}
		
		//variables = objectMapper.convertValue(jn.get("variables"), new TypeReference<Map<String, String>>() { }); 
		
		List<String> invalidNames = new ArrayList<>();
		for (String name : variables.keySet()) {
			if (!name.matches("[A-Za-z][A-Za-z0-9_]*"))
				invalidNames.add(name);
		}
		if (!invalidNames.isEmpty()) {
			throw new WoolAPIException("Invalid variable names: " + String.join(", ", invalidNames));
		}
		
		UserService userService = application.getServiceManager().getActiveUserService(userId);
		Map<String,Object> varStore = userService.variableStore
				.getModifiableMap(true, null);
		varStore.putAll(variables);
		return null;
	}
	
	// POST /start-dialogue
	public JsonNode startDialogue(JsonNode jn) throws WoolException, DatabaseException, IOException {
		// String userId, String dialogueName, String language
		String userId = jn.get("userId").asText();
		String dialogueName = jn.get("dialogueName").asText();
		String nodeId = jn.get("nodeId").asText();
		String language = jn.get("language").asText();
		
		if (nodeId == null || nodeId.equals("")) {
			nodeId = null;
		}
		
		
		//WoolNode startNode = application.getServiceManager().getActiveUserService(userId).startDialogue(dialogueName, nodeId, language);
		//ExecuteNodeResult enr = new ExecuteNodeResult(dialogueName, startNode, null, 0);
		ExecuteNodeResult enr = application.getServiceManager().getActiveUserService(userId).startDialogue(dialogueName, nodeId, language, new DateTime());
		DialogueMessage reply = DialogueMessageFactory.generateDialogueMessage(enr);
		return objectMapper.valueToTree(reply);
	}

	// GET /current-dialogue
	public JsonNode currentDialogue(JsonNode jn) throws WoolException, DatabaseException, IOException {
		String userId = jn.get("userId").asText();
		//WoolNode currentNode = application.getServiceManager().getActiveUserService(userId).currentDialogue();
		//String dialogueName = application.getServiceManager().getActiveUserService(userId).currentDialogueName();
		//if (currentNode == null)
		//	return null;
		//ExecuteNodeResult enr = new ExecuteNodeResult(dialogueName, currentNode, "", 0);
		//DialogueMessage reply = DialogueMessageFactory.generateDialogueMessage(enr);
		//return objectMapper.valueToTree(reply);
		return objectMapper.valueToTree(null);
	}
	
	// POST /back-dialogue
	public JsonNode backDialogue(JsonNode jn) throws WoolException, DatabaseException, IOException, WoolAPIException {
		String userId = jn.get("userId").asText();
		String loggedDialogueId = jn.get("loggedDialogueId").asText();
		int loggedInteractionIndex = jn.get("loggedInteractionIndex").asInt();
		
		UserService userService = application.getServiceManager()
				.getActiveUserService(userId);
		DialogueState state = userService.getDialogueState(loggedDialogueId, loggedInteractionIndex); // String loggedDialogueId, int loggedInteractionIndex
		DateTime now = new DateTime();
		
		DialogueMessage reply = new DialogueMessage();
		
		try {
			ExecuteNodeResult enr = userService.backDialogue(state, now);
			if (enr == null) 
				return null;
			reply = DialogueMessageFactory.generateDialogueMessage(enr);
		} catch (Exception e) {
			System.out.println("Exception going back in dialogue:\n"+e.getMessage());
			e.printStackTrace();
		}
		return objectMapper.valueToTree(reply);
	}

	// POST /progress-dialogue
	public JsonNode progressDialogue(JsonNode jn) throws WoolException, DatabaseException, IOException, WoolAPIException {
		String userId = jn.get("userId").asText();
		String loggedDialogueId = jn.get("loggedDialogueId").asText();
		int loggedInteractionIndex = jn.get("loggedInteractionIndex").asInt();
		int replyId = jn.get("replyId").asInt();
		// should we also confirm current nodeId?
		Map<String, Object> variables = new LinkedHashMap<>();
		//variables = objectMapper.convertValue(jn.get("variables"), new TypeReference<Map<String, ?>>() { });

		JsonNode varNode = jn.get("variables");

		if (varNode.isArray()) {
			for (JsonNode variableEntry : varNode) {
				String vname = variableEntry.get("name").asText();
				JsonNodeType type = variableEntry.get("value").getNodeType();
				
				if (type == JsonNodeType.NUMBER) {
					variables.put(vname, variableEntry.get("value").asDouble());
				} else if (type == JsonNodeType.STRING) {
					variables.put(vname, variableEntry.get("value").asText());
				} else if (type == JsonNodeType.BOOLEAN) {
					variables.put(vname, variableEntry.get("value").asBoolean());
				} else {
					throw new WoolAPIException("Cannot convert varaible type from store: "+vname+" ("+variableEntry.get("value").asText()+")");
				}
			}
		}
	      
		//variables = objectMapper.convertValue(jn.get("variables"), new TypeReference<Map<String, ?>>() { }); 

		UserService userService = application.getServiceManager()
				.getActiveUserService(userId);
		DialogueState state = userService.getDialogueState(loggedDialogueId, loggedInteractionIndex); // String loggedDialogueId, int loggedInteractionIndex
		DateTime now = new DateTime();
		if (!variables.isEmpty()) {
			userService.storeReplyInput(state, variables, now);
		}
		
		//WoolNode nextNode = application.getServiceManager().getActiveUserService(userId).progressDialogue(replyId);
		ExecuteNodeResult enr = userService.progressDialogue(state, replyId, now);
		//String currentDialogueName = application.getServiceManager().getActiveUserService(userId).....
		//if (!requestedDialogueName.equals(currentDialogueName)) {
		//	throw new WoolAPIException("Requested dialogue does not match current dialogue: " + requestedDialogueName + " != " + currentDialogueName);
		//}
		
		if (enr == null) 
			return null;
		//ExecuteNodeResult enr = new ExecuteNodeResult(requestedDialogueName, nextNode, "", 0);
		DialogueMessage reply = DialogueMessageFactory.generateDialogueMessage(enr);
		return objectMapper.valueToTree(reply);
	}

	// POST /cancel-dialogue
	public JsonNode cancelDialogue(JsonNode jn) throws DatabaseException, IOException {
		String userId = jn.get("userId").asText();
		String dialogueId = jn.get("dialogueId").asText();
		//String requestedDialogueName = jn.get("dialogueName").asText();
		application.getServiceManager().getActiveUserService(userId).cancelDialogue(dialogueId);
		// Respond OK?
		return null;
	}
	
}
