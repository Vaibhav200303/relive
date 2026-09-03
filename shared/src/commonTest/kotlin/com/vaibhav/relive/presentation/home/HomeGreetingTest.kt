package com.vaibhav.relive.presentation.home

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeGreetingTest {

    @Test
    fun namedGreetingUsesTheProfileDisplayName() {
        assertEquals("Welcome back, Alex", homeGreeting("Alex"))
    }

    @Test
    fun namelessGreetingIsExactlyWelcomeBack() {
        assertEquals("Welcome back", homeGreeting(null))
    }

    @Test
    fun blankNameIsTreatedAsNoName() {
        assertEquals("Welcome back", homeGreeting(""))
        assertEquals("Welcome back", homeGreeting("   "))
    }

    @Test
    fun profilePlaceholderNeverLeaksIntoTheGreeting() {
        // ProfileState substitutes "Your Relive" when no name is set. It is a Profile-screen
        // affordance, not a name: "Welcome back, Your Relive" must never render.
        assertEquals("Welcome back", homeGreeting("Your Relive"))
        assertEquals("Welcome back", homeGreeting("your relive"))
        assertEquals("Welcome back", homeGreeting("  Your Relive  "))
    }

    @Test
    fun surroundingWhitespaceIsTrimmedFromARealName() {
        assertEquals("Welcome back, Alex", homeGreeting("  Alex  "))
    }

    @Test
    fun aNameThatMerelyContainsThePlaceholderIsStillARealName() {
        assertEquals("Welcome back, Your Relive Companion", homeGreeting("Your Relive Companion"))
    }

    @Test
    fun subtitleIsFixed() {
        assertEquals("Your memories are waiting for you.", HOME_GREETING_SUBTITLE)
    }
}
