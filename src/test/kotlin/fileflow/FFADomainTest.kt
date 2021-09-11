package fileflow

import dk.brics.automaton.Automaton
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class FFADomainTest {
    @Test
    fun testAutomatonCompare() {
        val auto1 = Automaton.makeString("/file1")
        var auto2: Automaton? = auto1.clone()

        // assert automaton clone is equal
        Assertions.assertEquals(auto1, auto2)

        // add a file to the auto2 and check that they are no longer equal
        auto2 = auto1.union(Automaton.makeString("/file2"))
        Assertions.assertNotEquals(auto1, auto2)
    }
}