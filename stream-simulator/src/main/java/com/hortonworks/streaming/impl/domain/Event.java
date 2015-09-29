package com.hortonworks.streaming.impl.domain;

import org.apache.commons.lang.builder.ToStringBuilder;

public class Event {
	@Override
	public String toString() {
		return new ToStringBuilder(this).toString();
	}
}
