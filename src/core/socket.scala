/*
    Coaxial, version [unreleased]. Copyright 2023 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package coaxial

import nettlesome.*
import parasite.*
import fulminate.*
import turbulence.*
import rudiments.*
import anticipation.*
import perforate.*

import java.net as jn
import java.io as ji
import java.nio.channels as jnc
import java.nio.file as jnf

object UnixSocket

case class BindError() extends Error(msg"the port was not available for binding")

object DomainSocket:
  def apply[PathType: GenericPath](path: PathType): DomainSocket = DomainSocket(path.pathText)

case class DomainSocket(private[coaxial] val address: Text):
  def path[PathType: SpecificPath] = SpecificPath(address)

object Connectable:
  given domainSocket(using Raises[StreamError]): Connectable[DomainSocket] with
    type Output = LazyList[Bytes]
    case class Connection(channel: jnc.SocketChannel, in: ji.InputStream, out: ji.OutputStream)

    def connect(domainSocket: DomainSocket): Connection =
      val path = jnf.Path.of(domainSocket.address.s)
      val address = jn.UnixDomainSocketAddress.of(path)
      val channel = jnc.SocketChannel.open(jn.StandardProtocolFamily.UNIX).nn
      channel.connect(address)
      channel.finishConnect()
      val out = jnc.Channels.newOutputStream(channel).nn
      val in = jnc.Channels.newInputStream(channel).nn
      
      Connection(channel, in, out)

    def send(connection: Connection, input: Bytes): Unit =
      connection.out.write(input.mutable(using Unsafe))
      connection.out.flush()
      
    def receive(connection: Connection): LazyList[Bytes] =
      connection.in.stream[Bytes]
    
    def close(connection: Connection): Unit = connection.channel.close()

  given tcpPort(using Raises[StreamError]): Connectable[Endpoint[TcpPort]] with
    type Output = LazyList[Bytes]
    type Connection = jn.Socket
    
    def connect(endpoint: Endpoint[TcpPort]): jn.Socket =
      jn.Socket(jn.InetAddress.getByName(endpoint.remote.s), endpoint.port.number)
    
    def send(socket: jn.Socket, input: Bytes): Unit =
      val out = socket.getOutputStream.nn
      out.write(input.mutable(using Unsafe))
      out.flush()
    
    def close(socket: jn.Socket): Unit = socket.close()
    
    def receive(socket: jn.Socket): LazyList[Bytes] = socket.getInputStream.nn.stream[Bytes]

  given udpPort: Addressable[Endpoint[UdpPort]] with
    case class Connection(address: jn.InetAddress, port: Int, socket: jn.DatagramSocket)

    def connect(endpoint: Endpoint[UdpPort]): Connection =
      val address = jn.InetAddress.getByName(endpoint.remote.s).nn
      Connection(address, endpoint.port.number, jn.DatagramSocket())
    
    def send(connection: Connection, input: Bytes): Unit =
      val packet = jn.DatagramPacket(input.mutable(using Unsafe), input.length, connection.address,
          connection.port)
      
      connection.socket.send(packet)

trait Addressable[EndpointType]:
  type Connection

  def connect(endpoint: EndpointType): Connection
  def send(connection: Connection, input: Bytes): Unit

trait Connectable[EndpointType] extends Addressable[EndpointType]:
  def receive(connection: Connection): LazyList[Bytes]
  def close(connection: Connection): Unit

trait Bindable[SocketType]:
  type Binding
  type Input
  type Output
  
  def bind(socket: SocketType): Binding
  def connect(binding: Binding): Input
  def process(binding: Binding, input: Input, output: Output): Unit
  def close(connection: Input): Unit
  def stop(binding: Binding): Unit

case class UdpPacket(data: Bytes, sender: Ipv4 | Ipv6, port: UdpPort)

case class Connection(private[coaxial] val in: ji.InputStream, private[coaxial] val out: ji.OutputStream, close: () => Unit):
  def stream(): LazyList[Bytes] raises StreamError = in.stream[Bytes]

enum UdpResponse:
  case Ignore
  case Reply(data: Bytes)

object Bindable:

//   given domainSocket(using Raises[StreamError]): Bindable[DomainSocket] with
//     type Binding = jnc.ServerSocketChannel
//     type Output = LazyList[Bytes]
//     type Input = Connection

//     def bind(domainSocket: DomainSocket): jnc.ServerSocketChannel =
//       val address = jn.UnixDomainSocketAddress.of(domainSocket.address.s)
//       jnc.ServerSocketChannel.open(jn.StandardProtocolFamily.UNIX).nn.tap: channel =>
//         channel.configureBlocking(true)
//         channel.bind(address)
  
//     def connect(channel: jnc.ServerSocketChannel): Connection =
//       val clientChannel: jnc.SocketChannel = channel.accept().nn
//       val in = jnc.Channels.newInputStream(clientChannel).nn
//       val out = jnc.Channels.newOutputStream(clientChannel).nn
//       Connection(in, out)

//     def process(channel: jnc.ServerSocketChannel, connection: Connection, response: LazyList[Bytes]): Unit =
//       response.writeTo(connection.out)
//       connection.out.close()
    
//     def stop(channel: jnc.ServerSocketChannel): Unit = channel.close()

  given tcpPort(using Raises[StreamError]): Bindable[TcpPort] with
    type Binding = jn.ServerSocket
    type Output = LazyList[Bytes]
    type Input = Connection
    
    def bind(port: TcpPort): Binding = jn.ServerSocket(port.number)
    
    def connect(binding: Binding): Connection =
      val socket = binding.accept().nn
      Connection(socket.getInputStream.nn, socket.getOutputStream.nn, () => socket.close())
    
    def process(socket: jn.ServerSocket, connection: Connection, response: LazyList[Bytes]): Unit =
      response.writeTo(connection.out)
    
    def close(connection: Connection): Unit = connection.close()
    def stop(socket: jn.ServerSocket): Unit = socket.close()

  given udpPort: Bindable[UdpPort] with
    type Binding = jn.DatagramSocket
    type Output = UdpResponse
    type Input = UdpPacket
    
    def bind(port: UdpPort): Binding = jn.DatagramSocket(port.number)

    def connect(binding: Binding): UdpPacket =
      val array = new Array[Byte](1472)
      val packet = jn.DatagramPacket(array, 1472)
      val socket = binding.receive(packet)
      val address = packet.getSocketAddress.nn.asInstanceOf[jn.InetSocketAddress]
      val ip = address.getAddress.nn match
        case ip: jn.Inet4Address =>
          val bytes: Array[Byte] = ip.getAddress.nn
          Ipv4(bytes(0), bytes(1), bytes(2), bytes(3))
        case _                           => ??? // FIXME

      UdpPacket(array.slice(0, packet.getLength).immutable(using Unsafe), ip, UdpPort.unsafe(address.getPort))
    
    def process(socket: jn.DatagramSocket, input: UdpPacket, response: UdpResponse): Unit = response match
      case UdpResponse.Ignore => ()

      case UdpResponse.Reply(data) =>
        val sender = input.sender
        
        val ip: jn.InetAddress = input.sender match
          case ip: Ipv4 => jn.InetAddress.getByAddress(Array[Byte](ip.byte0.toByte, ip.byte1.toByte, ip.byte2.toByte, ip.byte3.toByte)).nn
          case _        => ??? // FIXME
        
        val packet = jn.DatagramPacket(data.mutable(using Unsafe), data.length, ip, input.port.number)
        socket.send(packet)
    
    def stop(binding: Binding): Unit = binding.close()
    def close(input: UdpPacket): Unit = ()

trait SocketService:
  def stop(): Unit

object Socket:
  def listen
      [SocketType]
      (socket: SocketType)
      [ResultType]
      (using bindable: Bindable[SocketType], monitor: Monitor)
      (fn: bindable.Input => bindable.Output)
      : SocketService raises BindError =

    val binding = bindable.bind(socket)
    var continue: Boolean = true
    
    val async = Async:
      while continue do
        val connection = bindable.connect(binding)
        Async(bindable.process(binding, connection, fn(connection)))

    new SocketService:
      def stop(): Unit =
        continue = false
        bindable.stop(binding)
        safely(async.await())

  // def sendTo
  //     [EndpointType]
  //     (endpoint: EndpointType)
  //     (using addressable: Addressable[EndpointType])
  //     (input: addressable.Input)
  //     (using Monitor)
  //     : Unit raises StreamError =
  //   val connection = addressable.connect(endpoint)
  //   val sender = Async(addressable.send(connection, input))
    
  //   safely(sender.await())
  
  // def connect
  //     [EndpointType]
  //     (endpoint: EndpointType)
  //     (using connectable: Connectable[EndpointType])
  //     (input: )
  //     [ResultType]
  //     (handle: connectable.Output => ResultType)
  //     (using Monitor)
  //     : ResultType raises StreamError =
  //   val connection = connectable.connect(endpoint)
  //   val sender = Async(connectable.send(connection, input))
    
  //   handle(connectable.receive(connection)).also:
  //     safely(sender.await())


extension [EndpointType](endpoint: EndpointType)
  def connect
      [StateType]
      (initialState: StateType)
      (initialMessage: Bytes = Bytes())
      (handle: (state: StateType) ?=> Bytes => Control[StateType])
      (using connectable: Connectable[EndpointType])
      : StateType =

    val connection = connectable.connect(endpoint)
    
    def recur(input: LazyList[Bytes], state: StateType): StateType = input match
      case head #:: tail => handle(using state)(head) match
        case Control.Terminate      => state
        case Control.Ignore         => recur(tail, state)
        case Control.Proceed(state) => recur(tail, state)
        
        case Control.Respond(message, state2) =>
          connectable.send(connection, message)
          recur(tail, state2.or(state))
      
      case _ => state
    
    recur(connectable.receive(connection), initialState).also:
      connectable.close(connection)

enum Control[+StateType]:
  case Terminate
  case Ignore
  case Proceed(state: StateType)
  case Respond(message: Bytes, state: Optional[StateType] = Unset)
