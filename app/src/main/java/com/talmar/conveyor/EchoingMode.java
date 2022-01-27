package com.talmar.conveyor;

public enum EchoingMode {
    ALL,  // All notifications are echoed
    GROUPS_ONLY,  // Only notifications with 2 or more authors are echoed
    DIRECT_MESSAGES_ONLY  // Only notifications with 1 or less authors are echoed
}
