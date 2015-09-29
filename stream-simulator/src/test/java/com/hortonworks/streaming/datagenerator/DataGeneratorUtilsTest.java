package com.hortonworks.streaming.datagenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class DataGeneratorUtilsTest {

	@Test
	public void testGetRandomIntBetween() {
		
		List<Integer> valueList = new ArrayList<Integer>();
		
		
		while (valueList.size() <= 90) {
			int value = DataGeneratorUtils.getRandomIntBetween(10, 100, valueList);
			valueList.add(value);
		}
		Collections.sort(valueList);
		for (Integer val: valueList) {
			System.out.println(val);
		}
	}
}
