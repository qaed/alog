package tsy;

//import static org.junit.Assert.*;

import nc.bs.logging.Logger;

//import org.junit.Test;

public class LogTest {

//	@Test
	public void test() {
		try{
			int i = 1/0;
		}catch (Exception e) {
			Logger.error("±¨´í:" + "123", e);
		}
	}

}
