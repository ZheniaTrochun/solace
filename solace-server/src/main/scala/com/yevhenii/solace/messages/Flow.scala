package com.yevhenii.solace.messages

case class Flow(
                 dlSrc: String,
                 dlDst: String,
                 dlType: String,
                 dlVlan: Int,
                 dlVlanPcp: Int,
                 nwSrc: String,
                 nwDst: String,
                 nwProto: Int,
                 tpSrc: String,
                 tpDst: String
               )

