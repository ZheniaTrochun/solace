package com.yevhenii.solace.config

import com.typesafe.config.Config

object ScalaConfig {

  implicit class asScalaConfig(config: Config) {
    private val stringClass = classOf[String]

    def getOrElse[A](path: String, default: A): A = {
      default.getClass match {
        case java.lang.Integer.TYPE => if (config.hasPath(path)) config.getInt(path).asInstanceOf[A] else default
        case java.lang.Long.TYPE => if (config.hasPath(path)) config.getLong(path).asInstanceOf[A] else default
        case java.lang.Boolean.TYPE => if (config.hasPath(path)) config.getBoolean(path).asInstanceOf[A] else default
        case a if a == stringClass => if (config.hasPath(path)) config.getString(path).asInstanceOf[A] else default
        case _ => if (config.hasPath(path)) config.getAnyRef(path).asInstanceOf[A] else default
      }
    }
  }
}
