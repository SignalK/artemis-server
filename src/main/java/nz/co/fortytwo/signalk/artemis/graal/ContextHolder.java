package nz.co.fortytwo.signalk.artemis.graal;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptException;

import org.graalvm.polyglot.Context;

public class ContextHolder {

	private Context graal;
	private Bindings nashorn;
	private Invocable engine;
	
	public ContextHolder(Context graal) {
		this.graal=graal;
	}
	
	
	public ContextHolder(Invocable inv, Bindings nashorn) {
		this.nashorn=nashorn;
		engine= inv;
	}

	public Throwable getMember(String string) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object invokeMember(String member, String method, Object arg) throws NoSuchMethodException, ScriptException {
		if(graal!=null) {
			return graal.getBindings("js").getMember(member).invokeMember(method, arg);
		}
		if(nashorn!=null) {
			return engine.invokeMethod(nashorn.get(member),method, arg);
		}
		return null;
	}
}
