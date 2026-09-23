package com.vaibhav.relive.presentation.home

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeGreetingTest {

    @Test
    fun namedGreetingUsesTheProfileDisplayName() {
        assertEquals("Hey, Alex!!!", homeGreeting("Alex"))
    }

    @Test
    fun greetingAddressesTheFirstNameOnly() {
        assertEquals("Hey, Vaibhav!!!", homeGreeting("Vaibhav Sharma"))
        assertEquals("Hey, Alex!!!", homeGreeting("Alex  van der Berg"))
    }

    @Test
    fun namelessGreetingIsExactlyHey() {
        assertEquals("Hey!!!", homeGreeting(null))
    }

    @Test
    fun blankNameIsTreatedAsNoName() {
        assertEquals("Hey!!!", homeGreeting(""))
        assertEquals("Hey!!!", homeGreeting("   "))
    }

    @Test
    fun profilePlaceholderNeverLeaksIntoTheGreeting() {
        // ProfileState substitutes "Your Relive" when no name is set. It is a Profile-screen
        // affordance, not a name: "Hey, Your Relive!!!" must never render.
        assertEquals("Hey!!!", homeGreeting("Your Relive"))
        assertEquals("Hey!!!", homeGreeting("your relive"))
        assertEquals("Hey!!!", homeGreeting("  Your Relive  "))
    }

    @Test
    fun surroundingWhitespaceIsTrimmedFromARealName() {
        assertEquals("Hey, Alex!!!", homeGreeting("  Alex  "))
    }

    @Test
    fun aNameThatMerelyContainsThePlaceholderIsStillARealName() {
        // Still greeted (not dropped like the exact placeholder), by its first word as always.
        assertEquals("Hey, Your!!!", homeGreeting("Your Relive Companion"))
    }

    @Test
    fun subtitleIsFixed() {
        assertEquals("Your memories are waiting for you.", HOME_GREETING_SUBTITLE)
    }
}
