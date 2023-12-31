GenCasterMessage {
	*addressRemoteAction {
		^"/remote/action";
	}

	*addressBeacon {
		^"/beacon";
	}

	*addressAcknowledge {
		^"/acknowledge";
	}

	*response {|uuid, status, returnValue=nil|
		^GenCasterMessage.eventToList((
			\uuid: uuid,
			\status: status,
			\returnValue: returnValue.cs,
		));
	}

	*remoteAction {|action, password, cmd, target=nil|
		// @todo check if action is speak or code
		^GenCasterMessage.eventToList((
			\protocol_version: "0.1",
			\action: action,
			\password: password,
			\cmd: cmd,
			\target: target,
		));
	}

	*beacon {|
		name,
		synthPort,
		langPort,
		janusOutPort,
		janusInPort,
		janusOutRoom,
		janusInRoom,
		janusPublicIp,
		useInput,
		oscBackendHost,
		oscBackendPort
	|
		^GenCasterMessage.eventToList((
			\name: name,
			\synth_port: synthPort,
			\lang_port: langPort,
			\janus_out_port: janusOutPort,
			\janus_in_port: janusInPort,
			\janus_out_room: janusOutRoom,
			\janus_in_room: janusInRoom,
			\janus_public_ip: janusPublicIp,
			\use_input: useInput,
			\osc_backend_host: oscBackendHost,
			\osc_backend_port: oscBackendPort,
		));
	}

	*eventToList {|event|
		var list = [];
		event.pairsDo({|k, v|
			list = list ++ [k];
			list = list ++ [v];
		});
		^list;
	}

	*eventToJson {|v|
		var val = case
		{v.class == Association} { GenCasterMessage.eventToJson.value(v.key) }
		{v===true} { "true" }
		{v===false} { "false" }
		{v.isString} { "\"%\"".format(v.replace("\"", "\\\"")).replace("\n", "\\n").replace("\t", " ") }
		{v.class == Symbol } { "\"%\"".format(v.replace("\"", "\\\"")).replace("\n", "\\n") }
		{v.isNil} {val = "null"}
		{v.isFloat} { "%".format(v)}
		{v.isInteger} { "%".format(v)}
		{v.isArray} {
			var jsonRepArray = v.collect{|i| GenCasterMessage.eventToJson.value(i)};
			val = "%".format(jsonRepArray);
		}
		{v.isFunction} { "\"a function\"" }
		{v.class == Event} {
			var string = "{";
			v.pairsDo({|k, v|
				string = string + "\"%\": %,".format(k, GenCasterMessage.eventToJson.(v));
			});
			// remove last ,
			string = string[0..string.size-2];
			string = string + "}";
			string;
		}
		{ "\"%\"".format(v.replace("\"", "\\\"")).replace("\n", "\\n") };
		val;
	}
}

GenCasterClient {
	var <name;
	var <netClient;
	var <password;

	*new {|name, netClient, password|
		^super.newCopyArgs(name, netClient, password).init;
	}

	init {}

	send {|cmd, action=\code|
		netClient.sendMsg(GenCasterMessage.addressRemoteAction, *GenCasterMessage.remoteAction(
			action: action,
			password: password,
			cmd: cmd,
			target: name
		));
	}

	speak{|text|
		this.send(cmd: text, action: \speak);
	}
}

GenCaster {
	classvar activeClients;

	var <hostname;
	var <port;
	var password;

	// own variables
	var <clients;
	var <netClient;
	var <servers;

	var <targets;

	*initClass {
		activeClients = ();
	}


	*new {|hostname="88.99.60.112", port=7000, password="demo"|
		^super.newCopyArgs(hostname, port, password).init;
	}

	init {
		netClient = NetAddr(hostname, port);
		clients = ();
		(0..15).do({|i|
			clients[i] = GenCasterClient(i, netClient, password);
		});
		targets = [nil];
	}

	sendAll {|code, action=\code|
		"send to all: '%'".format(code).postln;
		netClient.sendMsg(GenCasterMessage.addressRemoteAction, *GenCasterMessage.remoteAction(
			action: action,
			password: password,
			cmd: code,
		));
	}

	speakAll {|text|
		this.sendAll(code: text, action: \speak);
	}

	at {|k|
		^clients[k];
	}

	activate {|...ks|
		var targets = ks.collect({|k| this.at(k)});
		var interp;
		var fun = {|code|
			"send to '%': '%'".format(targets, code).postln;
			targets.do({|target|
				target.send(code);
			});
		};

		this.clear;
		interp = thisProcess.interpreter;
		interp.codeDump = interp.codeDump.addFunc(fun);
	}

	broadcastDocument {
		var interp, fun, document;
		this.clear;

		interp = thisProcess.interpreter;
		document = thisProcess.nowExecutingPath;

		if(document.isNil, {
			Error("Can only broadcast from a saved document").throw;
		});

		fun = {|code|
			var msg;

			if(ScIDE.currentPath == document, {
				var targetName = if(targets[0].isNil, {"all"}, {targets});
				"sending to %: '%'".format(targetName, code).postln;

				targets.do({|target|
					target.postln;
					netClient.sendMsg(GenCasterMessage.addressRemoteAction, *GenCasterMessage.remoteAction(
						action: \code,
						password: password,
						cmd: code,
						target: target,
					));
				});
				nil;
			}, {
				code;
			});
		};
		interp.preProcessor = fun;
	}

	broadcastDocumentReset {
		thisProcess.interpreter.preProcessor = nil;
	}

	broadcast {
		var interp, fun;
		this.clear;
		interp = thisProcess.interpreter;

		fun = {|code|
			var msg;
			"send to all: '%'".format(code).postln;
			netClient.sendMsg(GenCasterMessage.addressRemoteAction, *GenCasterMessage.remoteAction(
				action: \code,
				password: password,
				cmd: code,
			));
		};
		interp.codeDump = interp.codeDump.addFunc(fun);
	}

	targets_ {|v|
		// the nil target will not be deleted, therefore
		// sending it to all clients
		targets = if(v.isNil, {[nil]}, {v.asArray});
	}

	clear {
		var interp = thisProcess.interpreter;
		interp.codeDump = nil;
	}

}


GenCasterStatus {
	classvar <success;
	classvar <failure;
	classvar <ready;
	classvar <finished;
	classvar <beacon;
	classvar <received;

	*initClass {
		success = "SUCCESS";
		failure = "FAILURE";
		ready = "READY";
		finished = "FINISHED";
		beacon = "BEACON";
		received = "RECEIVED";
	}
}

GenCasterVerbosity {
	classvar <debug;
	classvar <info;
	classvar <warning;
	classvar <error;

	*initClass {
		debug = 10;
		info = 20;
		warning = 30;
		error = 40;
	}
}

GenCasterServer {
	var <name;
	var <synthPort;
	var <langPort;

	var <janusOutPort;
	var <janusInPort;
	var <janusOutRoom;
	var <janusInRoom;
	var <janusPublicIP;

	var <useInput;

	var <oscBackendHost;
	var <oscBackendPort;


	var <>verbosity;

	// private
	var <>oscBackendClient;
	var <>environment; // shall this be a proxy space?
	var <>server;
	var <beacon;

	// basically a constructor which allows us to set
	// the necessary values directly or via env variables
	// as fallback - a "bit" verbose, but well
	*new {arg
		name = nil,
		synthPort = nil,
		langPort = nil,
		janusOutPort = nil,
		janusInPort = nil,
		janusOutRoom = nil,
		janusInRoom = nil,
		janusPublicIP = nil,
		useInput = nil,
		oscBackendHost = nil,
		oscBackendPort = nil,
		verbosity = nil;

		name = name ? "SC_NAME".getenv ? "GENCASTER_LOCAL";
		synthPort = synthPort ? ("SC_SYNTH_PORT".getenv ? 57110).asInteger;
		langPort = langPort ? NetAddr.langPort ? 57120;
		janusOutPort = janusOutPort ? ("JANUS_OUT_PORT".getenv ? 0).asInteger;
		janusInPort = janusInPort ? ("JANUS_IN_PORT".getenv ? 0).asInteger;
		janusOutRoom = janusOutRoom ? "JANUS_OUT_ROOM".getenv;
		janusInRoom = janusInRoom ? "JANUS_IN_ROOM".getenv;
		janusPublicIP = janusPublicIP ? "JANUS_PUBLIC_IP".getenv;
		useInput = useInput ? ("SUPERCOLLIDER_USE_INPUT".getenv ? 0).asInteger;
		oscBackendHost = oscBackendHost ? "BACKEND_OSC_HOST".getenv ? "127.0.0.1";
		oscBackendPort = oscBackendPort ? ("BACKEND_OSC_PORT".getenv ? 8082).asInteger;
		verbosity = verbosity ? ("SC_VERBOSITY".getenv ? GenCasterVerbosity.info).asInteger;

		^super.newCopyArgs(
			name,
			synthPort,
			langPort,
			janusOutPort,
			janusInPort,
			janusOutRoom,
			janusInRoom,
			janusPublicIP,
			useInput,
			oscBackendHost,
			oscBackendPort,
			verbosity,
		).init;
	}

	init {
		oscBackendClient = NetAddr(
			hostname: oscBackendHost,
			port: oscBackendPort,
		);
		environment = this.serverInfo;
		environment[\oscBackendClient] = oscBackendClient;
		environment[\this] = this;
		this.loadSynthDefs;
		beacon = SkipJack(
			updateFunc: {
				this.sendAck(
					status: GenCasterStatus.beacon,
					uuid: 0,
					message: this.serverInfo,
					address: "/beacon",
				);
			},
			dt: 5.0,
			name: "gencasterBeacon",
			autostart: false,
		);
		// using SkipJack for the recover of our responder
		// is too slow or a too heavy load, therefore we
		// add a callback to the CmdPeriod request which
		// resurrects the instruction receiver
		CmdPeriod.add({this.instructionReceiver.value}.defer(0.01));
	}

	loadSynthDefs {
		// can playback mono and stereo files as stereo files
		SynthDef(\gencasterBufferPlayback, {|out|
			var buffer = \buffer.kr(0);
			var numChannels = BufChannels.kr(buffer);
			var sig = PlayBuf.ar(
				numChannels: 2,
				bufnum: buffer,
				rate: BufRateScale.kr(buffer) * \rate.kr(1.0),
				doneAction: Done.freeSelf
			);
			sig = Select.ar(numChannels>1, [sig[0]!2, sig]);
			Out.ar(out, sig * \amp.kr(0.2));
		}).add;
	}

	playBuffer {|bufferPath, callback=nil|
		// loads, plays and frees a buffer
		// this allows us to establish garbage collection as otherwise
		// we would run out of memory and number of buffers
		callback = callback?{};
		Buffer.read(Server.default, bufferPath, action: {|buffer|
			Synth(\gencasterBufferPlayback, [\buffer, buffer]).onFree({
				buffer.free;
				callback.(buffer);
			});
		});
	}

	syncPlayBuffer {|bufferPath, uuid|
		this.playBuffer(bufferPath, callback: {
			this.sendAck(
				status: GenCasterStatus.finished,
				uuid: uuid,
				message: (return_value: "Buffer % finished playing".format(bufferPath); ),
			);
		});
	}

	num {
		synthPort%16;
	}

	serverInfo {
		^(
			name: name,
			synth_port: synthPort,
			lang_port: langPort,
			janus_out_port: janusOutPort,
			janus_in_port: janusInPort,
			janus_out_room: janusOutRoom,
			janus_in_room: janusInRoom,
			janus_public_ip: janusPublicIP,
			use_input: useInput,
			osc_backend_host: oscBackendHost,
			osc_backend_port: oscBackendPort,
		);
	}

	*eventToArray {|event|
		// as SC does not have a nice way to create a JSON we simply send an
		// array in [key, value, key, value] format
		var array = event.keys.collect({|key| [key, event[key]]}).asArray.flatten;
		^array;
	}

	sendAck {|status, uuid=nil, message=nil, address=nil|
		message = message ? ();
		message[\uuid] = uuid ? 0;
		message[\status] = status;
		address = address ? "/acknowledge";

		if(verbosity<=GenCasterVerbosity.debug, {
			if(status!=GenCasterStatus.beacon, {
				"%\t%\t%".format(status, message.uuid, message).postln;
			});
		});
		oscBackendClient.sendMsg(address, *GenCasterServer.eventToArray(message));

	}

	postServerInfo {
		"### GenCaster server ###".postln;
		this.serverInfo.pairsDo({|k, v|
			"%: %".format(k, v).postln;
		});
		"### /GenCaster server ###".postln;
	}

	startServer {
		server = Server(
			name: name,
			addr: NetAddr(hostname: "127.0.0.1", port: synthPort),
		);
		server.options.sampleRate_(48000).memoryLocking_(true).memSize_(8192*4);
		server.options.numOutputBusChannels =2;
		server.options.device = "default:%".format(name);
		server.options.bindAddress = "0.0.0.0";
		server.options.maxLogins = 2;
		Server.default = server;

		"Booting server % on port %".format(name, synthPort).postln;
		server.waitForBoot(onComplete: this.postStartServer);
	}

	instructionReceiver {
		"Starting instruction receiver".postln;
		^OSCFunc({|msg, time, addr, recvPort|
			var uuid = msg[1];
			var function = (msg[2] ? "{}").asString;
			var manualFinish = msg[3] ? false;
			this.sendAck(status: GenCasterStatus.received, uuid: uuid);
			"Execute now: '%'".format(function).postln;
			{
				var returnValue = function.interpret;
				if(returnValue.class==Function, {
					returnValue = function.interpret.(environment);
				});

				if(returnValue.class==Event, {
					returnValue = GenCasterMessage.eventToJson(returnValue);
				});

				if(manualFinish.not) {
					this.sendAck(
						status: GenCasterStatus.finished,
						uuid: uuid,
						message: (return_value: returnValue.cs ),
					);
				}
			}.fork;
		}, path: "/instruction");
	}

	postStartServer {
		"Finished booting server".postln;
		"Start beacon".postln;
		beacon.play;
		this.instructionReceiver;
	}
}
