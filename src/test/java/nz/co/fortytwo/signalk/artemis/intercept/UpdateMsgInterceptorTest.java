package nz.co.fortytwo.signalk.artemis.intercept;

import static org.junit.Assert.*;

import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class UpdateMsgInterceptorTest  extends EasyMockSupport {

	@Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Mock
    private DeltaMsgInterceptor interceptor;// 1

    @Before
    public void before(){
    	interceptor = partialMockBuilder(DeltaMsgInterceptor.class)
	    	.addMockedMethod("sendSourceMsg")
	    	.addMockedMethod("sendMsg").createMock(); 
    }
	@Test
	public void shouldCallSendMsg() {
//		interceptor.parseUpdate(json, ctx,mess);
	}

}
