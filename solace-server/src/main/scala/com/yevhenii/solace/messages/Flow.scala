package com.yevhenii.solace.messages

case class Flow(
                 dlSrc: String,
                 dlDst: String,
                 dlType: Int,
                 dlVlan: Int,
                 dlVlanPcp: Int,
                 nwSrc: String,
                 nwDst: String,
                 nwProto: Int,
                 tpSrc: String,
                 tpDst: String
               )

