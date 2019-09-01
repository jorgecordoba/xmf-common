package com.devfields.xmf.common.messaging

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Created by jco27 on 24/02/2017.
 */
class MessageContext
    @JsonCreator
    constructor(val auditTrailId: String, val domain: String, val tenant: String, val priority: Int) {
    constructor(auditTrailId: String, domain: String, tenant: String) : this(auditTrailId, domain, tenant, 1)
    constructor(auditTrailId: String, domain: String) : this(auditTrailId, domain, "default-tenant")
    constructor(auditTrailId: String) : this(auditTrailId, "default-domain")
}