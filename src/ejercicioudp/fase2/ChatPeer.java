package ejercicioudp.fase2;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ChatPeer {

    // Configuración
    private static final int PORT = 9877;
    private static final String BROADCAST_IP = "10.59.43.255";
    private static String miNombreChat;

    private static List<InetSocketAddress> amigos = new ArrayList<>();
    private static InetAddress miIP; // Para Filtrado

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Introduce tu nombre para el Chat: ");
        miNombreChat = scanner.nextLine();
        try {
            miIP = InetAddress.getLocalHost();
            System.out.println("--- INICIANDO CHAT PEER ---");
            System.out.println("Mi IP Local: " + miIP.getHostAddress());

            // 1. Iniciar el hilo de escucha continua (El Servidor)
            Thread listenerThread = new Thread(new ListenerTask());
            listenerThread.start();

            // 2. Iniciar el cliente/enviador (Descubrimiento y Bucle de Chat)
            iniciarChatClient(listenerThread);

        } catch (UnknownHostException e) {
            System.err.println("Error al obtener la IP local: " + e.getMessage());
        }
    }

    static class ListenerTask implements Runnable {
        private DatagramSocket serverSocket;

        @Override
        public void run() {
            try {
                serverSocket = new DatagramSocket(PORT);
                System.out.println("Servidor/Peer escuchando en puerto " + PORT + "...");

                byte[] receiveData = new byte[1024];

                while (true) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);

                    String tramaRecibida = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                    InetAddress senderIP = receivePacket.getAddress();
                    int senderPort = receivePacket.getPort();

                    // 1. Filtrado (AP 3.7): Ignorar paquetes si vienen de mí mismo
                    if (senderIP.equals(miIP) || senderIP.isLoopbackAddress()) {
                        continue;
                    }

                    procesarTrama(tramaRecibida, senderIP, senderPort, serverSocket);
                }
            } catch (SocketException e) {
                // Esto es normal cuando el socket se cierra al terminar el programa
                System.out.println("\n[LISTENER] Socket cerrado. Terminando escucha.");
            } catch (IOException e) {
                System.err.println("Error de I/O en Listener: " + e.getMessage());
            } finally {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            }
        }

        private void procesarTrama(String trama, InetAddress ip, int port, DatagramSocket socket) throws IOException {

            // Validación Robusta (AP 3.6): @...#...# (o similar, adaptado al nuevo chat)
            if (trama.startsWith("@") && trama.endsWith("@")) {
                String contenido = trama.substring(1, trama.length() - 1);

                if (contenido.contains("#")) {
                    String[] partes = contenido.split("#", 2);
                    String comando = partes[0].trim().toLowerCase();
                    String dato = partes[1];

                    InetSocketAddress senderAddress = new InetSocketAddress(ip, port);

                    switch (comando) {
                        case "hola":
                            // Fase 1: Saludo
                            System.out.println("\n[PROTO]: Saludo recibido de: " + dato);
                            enviarRespuesta(socket, "@ok#" + miNombreChat + "@", ip, port);
                            break;

                        case "descub":

                            agregarAmigo(senderAddress, dato);

                            String respDescub = "@amigo#" + miNombreChat + "@";
                            enviarRespuesta(socket, respDescub, ip, port);
                            break;

                        case "amigo":

                            agregarAmigo(senderAddress, dato);
                            break;

                        case "msg":

                            System.out.println("\n[CHAT] " + dato);
                            System.out.print("[" + miNombreChat + "]> ");
                            break;

                        default:
                            System.out.println("[LOG ERROR]: Comando desconocido: " + comando + ". Ignorando...");
                    }
                } else {
                    System.out.println("[LOG ERROR]: Trama incorrecta (falta #). Ignorando: " + trama);
                }
            } else {
                System.out.println("[LOG ERROR]: Trama incorrecta (falta @). Ignorando: " + trama);
            }
        }

        private void enviarRespuesta(DatagramSocket socket, String mensaje, InetAddress ip, int port) throws IOException {
            byte[] sendData = mensaje.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
            socket.send(sendPacket);
        }

        private void agregarAmigo(InetSocketAddress address, String nombre) {
            if (!amigos.contains(address)) {
                amigos.add(address);
                System.out.println("\n[DISCOVERY]: Encontrado nuevo par: " + nombre + " (" + address.getAddress().getHostAddress() + ")");
                System.out.println("Amigos actuales: " + amigos.size());
            }
        }
    }


    private static void iniciarChatClient(Thread listenerThread) {

        try (DatagramSocket clientSocket = new DatagramSocket();
             Scanner scanner = new Scanner(System.in)) {

            // Establecer que el socket puede enviar Broadcasts (necesario en algunos OS)
            clientSocket.setBroadcast(true);


            System.out.println("\n--- INICIANDO DESCUBRIMIENTO ---");
            String broadcastMsg = "@descub#" + miNombreChat + "@";

            byte[] sendData = broadcastMsg.getBytes();
            InetAddress broadcastIP = InetAddress.getByName(BROADCAST_IP);

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcastIP, PORT);
            clientSocket.send(sendPacket);
            System.out.println("Enviado Broadcast de descubrimiento a la red.");


            Thread.sleep(1000);

            System.out.println("\n*** Listado de Amigos Encontrados (" + amigos.size() + ") ***");
            for (InetSocketAddress friend : amigos) {
                System.out.println(" -> Par en: " + friend.getAddress().getHostAddress());
            }


            System.out.println("\n--- CHAT INICIADO ---");
            System.out.println("Escribe tu mensaje. Escribe 'salir' para terminar.");

            while (!Thread.interrupted()) {
                System.out.print("[" + miNombreChat + "]> ");
                String mensajeUsuario = scanner.nextLine();

                if (mensajeUsuario.equalsIgnoreCase("salir")) {
                    break;
                }


                String tramaChat = "@msg#" + miNombreChat + ": " + mensajeUsuario + "@";
                byte[] chatData = tramaChat.getBytes();


                if (amigos.isEmpty()) {
                    System.out.println("[INFO]: No hay interlocutores. Intenta ejecutar otro ChatPeer.");
                }

                for (InetSocketAddress friend : amigos) {
                    DatagramPacket chatPacket = new DatagramPacket(chatData, chatData.length, friend.getAddress(), friend.getPort());
                    clientSocket.send(chatPacket);
                }
            }

        } catch (IOException e) {
            System.err.println("Error de I/O en Cliente: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            System.out.println("Saliendo del chat. Deteniendo Listener.");
            if (listenerThread != null) {
                listenerThread.interrupt();
            }
        }
    }
}