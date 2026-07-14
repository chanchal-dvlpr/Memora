package com.contextengine.test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Base class for unit tests.
 * Runs without a Spring Boot container for speed and isolation.
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {
}
