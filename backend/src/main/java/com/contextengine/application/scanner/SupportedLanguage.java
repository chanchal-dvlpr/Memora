package com.contextengine.application.scanner;

/**
 * Enumeration of supported programming languages in the Context Engine workspace scanner.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 * <p>
 * Future Usage: Used by the parser coordinator to load appropriate AST parser configurations.
 * </p>
 */
public enum SupportedLanguage {

    /**
     * Java programming language.
     */
    JAVA,

    /**
     * Python programming language.
     */
    PYTHON,

    /**
     * JavaScript programming language.
     */
    JAVASCRIPT,

    /**
     * TypeScript programming language.
     */
    TYPESCRIPT,

    /**
     * C++ programming language.
     */
    CPP,

    /**
     * Go programming language.
     */
    GO,

    /**
     * Bash scripting language.
     */
    BASH,

    /**
     * Shell scripting language.
     */
    SHELL,

    /**
     * Ruby programming language.
     */
    RUBY,

    /**
     * Perl programming language.
     */
    PERL,

    /**
     * PHP programming language.
     */
    PHP,

    /**
     * Lua programming language.
     */
    LUA,

    /**
     * Fallback for unsupported or plain text file types.
     */
    UNSUPPORTED
}
