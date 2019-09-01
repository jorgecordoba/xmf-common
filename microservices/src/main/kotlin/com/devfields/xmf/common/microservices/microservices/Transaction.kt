package com.devfields.xmf.common.microservices.microservices

import javax.jms.Session

/**
 * Created by jco27 on 24/02/2017.
 */
class Transaction(internal val session : Session) {

    fun commit(){
        session.commit()
    }

    fun rollback(){
        session.rollback()
    }

}