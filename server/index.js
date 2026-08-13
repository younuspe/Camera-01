const http = require('http');
const fs = require('fs');
const path = require('path');
const { WebSocketServer, WebSocket } = require('ws');

const PORT = process.env.PORT || 8080;

// HTTP Server to serve index.html
const server = http.createServer((req, res) => {
  let filePath = path.join(__dirname, 'public', req.url === '/' ? 'index.html' : req.url);
  fs.readFile(filePath, (err, content) => {
    if (err) {
      res.writeHead(404);
      res.end('File not found');
    } else {
      res.writeHead(200, { 'Content-Type': 'text/html' });
      res.end(content, 'utf-8');
    }
  });
});

const wss = new WebSocketServer({ server });

let cameraSocket = null;
let controllerSocket = null;

wss.on('connection', (ws) => {
  console.log('Client connected');

  ws.on('message', (message) => {
    try {
      const data = JSON.parse(message);

      switch (data.type) {
        case 'register':
          if (data.role === 'camera') {
            cameraSocket = ws;
            console.log('Camera device registered');
          } else if (data.role === 'controller') {
            controllerSocket = ws;
            console.log('Controller device registered');
          }
          break;

        case 'offer':
        case 'answer':
        case 'candidate':
          relayMessage(ws, data);
          break;

        case 'command':
          if (ws === controllerSocket && cameraSocket) {
            cameraSocket.send(JSON.stringify(data));
            console.log(`Relayed command to camera: ${data.action}`);
          }
          break;
      }
    } catch (err) {
      console.error('Error handling message:', err);
    }
  });

  ws.on('close', () => {
    if (ws === cameraSocket) cameraSocket = null;
    if (ws === controllerSocket) controllerSocket = null;
  });
});

function relayMessage(sender, data) {
  const recipient = sender === cameraSocket ? controllerSocket : cameraSocket;
  if (recipient && recipient.readyState === WebSocket.OPEN) {
    recipient.send(JSON.stringify(data));
  }
}

server.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
