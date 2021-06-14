package com.github.ljtfreitas.julian;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationHandler;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.DefaultInvocationHandler;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.EndpointDefinition;
import com.github.ljtfreitas.julian.EndpointDefinition.Path;
import com.github.ljtfreitas.julian.ProxyFactory;
import com.github.ljtfreitas.julian.RequestRunner;
import com.github.ljtfreitas.julian.contract.Contract;

@ExtendWith(MockitoExtension.class)
class ProxyFactoryTest {

	@Mock(answer = RETURNS_DEEP_STUBS)
	Contract contract;

	@Mock
	RequestRunner requestRunner;

	InvocationHandler invocationHandler;

	ProxyFactory<MyApiType> proxyFactory;

	@BeforeEach
	void setup() {		
		this.proxyFactory = new ProxyFactory<>(MyApiType.class, new DefaultInvocationHandler(contract, requestRunner));
	}

	@Test
	void simple() {
		Endpoint endpoint = new EndpointDefinition(new Path("http://my.api.com"));

		when(contract.endpoints().select(any())).then(a -> Optional.of(endpoint));
		when(requestRunner.run(endpoint, Arguments.create("argument"))).thenReturn("expected");

		MyApiType myApiType = proxyFactory.create();

		assertEquals("expected", myApiType.method("argument"));
	}

	@Test
	void defaultMethod() {
		when(contract.endpoints().select(any())).thenReturn(Optional.empty());

		MyApiType myApiType = proxyFactory.create();

		assertEquals("default method", myApiType.defaultMethod());
	}

	@Test
	void staticMethod() {
		assertEquals("static method", MyApiType.staticMethod());
	}

	interface MyApiType {

		String method(String argument);

		default String defaultMethod() {
			return "default method";
		}

		static String staticMethod() {
			return "static method";
		}
	}
}
