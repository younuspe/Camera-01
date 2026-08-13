const { WebSocketServer, WebSocket } = require('ws');

const PORT = process.env.PORT || 8080;
const wss = new WebSocketServer({ port: PORT });

// Track connected roles: 'camera' or 'controller'
let cameraSocket = null;
let controllerSocket = null;

console.log(`Signaling server running on port ${PORT}`);

wss.on('connection', (ws) => {
  console.log('New client connected');

  ws.on('message', (message) => {
    try {
      const data = JSON.parse(message);

      switch (data.type) {
        // Register client role
        case 'register':
          if (data.role === 'camera') {
            cameraSocket = ws;
            console.log('Camera device registered');
          } else if (data.role === 'controller') {
            controllerSocket = ws;
            console.log('Controller device registered');
          }
          break;

        // Relay WebRTC signaling (offers, answers, ICE candidates)
        case 'offer':
        case 'answer':
        case 'candidate':
          relayMessage(ws, data);
          break;

        // Relay camera control commands (e.g., switch camera, toggle flash)
        case 'command':
          if (ws === controllerSocket && cameraSocket) {
            cameraSocket.send(JSON.stringify(data));
            console.log(`Relayed command to camera: ${data.action}`);
          }
          break;

        default:
          console.log('Unknown message type:', data.type);
      }
    } catch (err) {
      console.error('Failed to parse message:', err);
    }
  });

  ws.on('close', () => {
    if (ws === cameraSocket) {
      console.log('Camera disconnected');
      cameraSocket = null;
    } else if (ws === controllerSocket) {
      console.log('Controller disconnected');
      controllerSocket = null;
    }
  });
});

function relayMessage(sender, data) {
  const recipient = sender === cameraSocket ? controllerSocket : cameraSocket;
  if (recipient && recipient.readyState === WebSocket.OPEN) {
    recipient.send(JSON.stringify(data));
  }
}
