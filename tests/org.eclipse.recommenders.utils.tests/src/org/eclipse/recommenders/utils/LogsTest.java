package org.eclipse.recommenders.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.recommenders.utils.Logs.ILogMessage;
import org.junit.Before;
import org.junit.Test;

public class LogsTest {

    private ILogMessage msg;

    @Before
    public void before() {
        msg = mock(Logs.ILogMessage.class);
        when(msg.bundle()).thenReturn(Logs.getBundle(getClass()));

    }

    @Test
    public void testNullMessage() {
        Logs.toStatus(msg, null, null);
    }

    @Test
    public void testNullArgumentsArray() {
        when(msg.message()).thenReturn("{0}");
        IStatus actual = Logs.toStatus(msg, null, null);
        assertEquals("{0}", actual.getMessage());
    }

    @Test
    public void testNullArguments() {
        when(msg.message()).thenReturn("{0}");
        IStatus actual = Logs.toStatus(msg, null, new Object[] { null });
        assertEquals("null", actual.getMessage());
    }

    @Test
    public void testWrongFormatString() {
        when(msg.message()).thenReturn("{}");
        IStatus actual = Logs.toStatus(msg, null, new Object[] { null });
        assertEquals("{}", actual.getMessage());
    }

    @Test
    public void testToFewArguments() {
        when(msg.message()).thenReturn("{0} {1}");
        IStatus actual = Logs.toStatus(msg, null, new Object[] { null });
        assertEquals("null {1}", actual.getMessage());
    }

}
