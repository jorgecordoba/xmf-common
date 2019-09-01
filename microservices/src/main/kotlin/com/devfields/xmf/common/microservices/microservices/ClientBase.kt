package com.devfields.xmf.common.microservices.microservices

import com.devfields.xmf.common.configuration.configuration.ConfigurationStore
import com.devfields.xmf.common.logging.XmfLoggerFactory
import com.devfields.xmf.common.types.Version
import com.codahale.metrics.MetricRegistry
import javax.jms.Session

/**
 * Created by jco27 on 02/01/2017.
 */
open class ClientBase @JvmOverloads constructor(configurationStore: ConfigurationStore,
                                                serviceName : String,
                                                val serviceVersion : Version,
                                                instanceName : String,
                                                metricsReg: MetricRegistry = MetricRegistry()) : CommsBase(configurationStore, serviceName, instanceName) {

    private val operations : MutableList<ApiOperation<out Command>> = mutableListOf()
    private val metrics : MetricRegistry = metricsReg
    private val logger = XmfLoggerFactory.getLogger(this::class.java)

    private fun send(cmd : Command, session: Session){
        synchronized(this.session) {
            if (cmd.header?.destination?.name != null) {
                val dest = session.createQueue(configuration.readValue("services.$serviceName.location", serviceName))
                val producer = session.createProducer(dest)
                var msg = createMessage(cmd)
                producer.send(msg)
            } else {
                throw IllegalArgumentException("Destination can't be null")
            }
        }
    }

    protected fun send(cmd : Command){
        send(cmd, this.session)
        this.session.commit()
    }

    protected fun send(cmd: Command, transaction : Transaction) {
        logger.xmf_trace_log(serviceName, instanceName, "Sending message as part of the transacted session ${transaction.session.hashCode()}")
        send(cmd, transaction.session)
    }

    protected fun <T : Response>registerResponse(source: String, classReference : Class<T>, handler: CommandHandler<T>) {
        var op = ApiOperation(configuration, source, serviceVersion, instanceName, "${this.serviceName}.${classReference.simpleName}", classReference, handler)
        operations.add(op)
        op.start()
    }

    override fun stop() {
        operations.forEach { it.stop() }
        super.stop()
    }
}