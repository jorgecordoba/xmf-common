package com.devfields.xmf.common.microservices.microservices

import com.devfields.xmf.common.configuration.configuration.ConfigurationStore
import com.devfields.xmf.common.logging.XmfLoggerFactory
import com.devfields.xmf.common.types.Version
import com.codahale.metrics.MetricRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import javax.jms.Session
import kotlin.math.log

/**
 * Created by jco27 on 02/01/2017.
 */
class ClientBase @JvmOverloads constructor(configurationStore: ConfigurationStore,
                                           serviceName : String,
                                           val serviceVersion : Version,
                                           instanceName : String,
                                           metricsReg: MetricRegistry = MetricRegistry()) : CommsBase(configurationStore, serviceName, instanceName) {

    private val operations : MutableList<ApiOperation<out Command>> = mutableListOf()
    private val metrics : MetricRegistry = metricsReg
    private val logger = XmfLoggerFactory.getLogger(this::class.java)
    private val listeners = mutableListOf<String>()
    private val completions = mutableMapOf<String, MutableList<CompletableDeferred<Response>>>()

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

    suspend fun <T: Response>sendAndWait(cmd: Command) : T? {
        val cmdId = cmd.getUniqueId()
        if (!completions.containsKey(cmdId)) {
            completions[cmdId] = mutableListOf()
        }
        val completableDeferred = CompletableDeferred<Response>()
        completions[cmdId]!!.add(completableDeferred)

        send(cmd)
        var response = completableDeferred.await() as? T
        if (response == null) {
            logger.warn(cmd.header.context, serviceName, instanceName, "Received response for command ${cmd.header.type} to ${cmd.header.destination} and id ${cmdId} which was not of expected type")
        }
        return response
    }

    private fun internalResponseHandler(response : Response) : HandleAction {
        var coroutinesWaiting = completions[response.requestId]
        if (null == coroutinesWaiting || coroutinesWaiting.size == 0) {
            logger.warn(response.header.context, serviceName, instanceName, "Received message without coroutine handler")
            return HandleAction.BadMsg
        }

        coroutinesWaiting.forEach { it.complete(response)}

        return HandleAction.Commit;
    }

    protected fun <T : Response>sendAsync(cmd: Command, classReference:Class<T>) : Deferred<T?> {
        if (!listeners.contains(classReference.name)) {
            var op = ApiOperation(configuration, serviceName, serviceVersion, instanceName, "${this.serviceName}.${classReference.simpleName}", classReference, this::internalResponseHandler)
            operations.add(op)
            op.start()
        }

        return  GlobalScope.async { sendAndWait<T>(cmd) }
    }

    override fun stop() {
        operations.forEach { it.stop() }
        super.stop()
    }
}