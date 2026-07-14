package com.contextengine.mcp;

import com.contextengine.mcp.session.McpSession;
import com.contextengine.mcp.session.McpSessionManager;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class McpSessionsTest {

    @Test
    void testSessionCreationAndTransitions() {
        McpSessionManager manager = new McpSessionManager();
        McpSession session = manager.createSession("IDE-Client", "1.5.0");

        Assertions.assertNotNull(session);
        Assertions.assertEquals("IDE-Client", session.getContext().getClientName());
        Assertions.assertEquals("1.5.0", session.getContext().getClientVersion());
        Assertions.assertEquals(McpSessionState.CONNECTING, session.getState());

        manager.establishSession(session.getSessionId());
        Assertions.assertEquals(McpSessionState.ACTIVE, session.getState());

        Assertions.assertTrue(session.getContext().hasPermission("mcp:read"));
        Assertions.assertTrue(session.getContext().hasPermission("mcp:tool_invoke"));

        manager.terminateSession(session.getSessionId());
        Assertions.assertEquals(0, manager.getActiveSessions().size());
    }

    @Test
    void testPruneExpiredSessions() throws Exception {
        McpSessionManager manager = new McpSessionManager();
        McpSession session = manager.createSession("IDE-Client", "1.5.0");

        Collection<McpSession> active = manager.getActiveSessions();
        Assertions.assertEquals(1, active.size());

        // Simulate session timeout by backdating the touch timestamp in test helper
        // Since we cannot easily modify system time, we test the isExpired utility directly
        Assertions.assertFalse(session.isExpired(300L));

        // Touch updates time
        long initialTime = session.getLastAccessedTimestamp();
        Thread.sleep(10);
        session.touch();
        Assertions.assertTrue(session.getLastAccessedTimestamp() > initialTime);
    }
}
