package com.hortonworks.streaming.impl.domain;

import java.io.Serializable;
import java.util.logging.Logger;

import org.apache.commons.lang.builder.ToStringBuilder;

import akka.actor.UntypedActor;

import com.hortonworks.streaming.interfaces.DomainObject;

public abstract class AbstractDomainObject extends UntypedActor implements DomainObject,
		Serializable {
	private static final long serialVersionUID = -2630503054916573455L;
	protected Logger logger = Logger.getLogger(this.getClass().toString());

	@Override
	public String toString() {
		return new ToStringBuilder(this).toString();
	}
}
