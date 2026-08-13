const express = require('express');
const http = require('http');
const path = require('path');
const { WebSocketServer } = require('ws');

const PORT = process.env.PORT || 8080;

// 1. Initialize Express App & Serve Static Files
const app = express();
app.use(express.static(path.join(__dirname, 'public')));

// 2. Attach Express to the HTTP Server
const server = http.createServer(app);
const wss = new WebSocketServer({ server });

let cameraSocket = null;
let controllerSocket = null;

// 3. WebSocket Connection Handling
wss.on('connection', (ws) => {
    console.log('Client connected');

    ws.on('message', (message) => {
        try {
            const data = JSON.parse(message);

            switch (data.type) {
                case 'register':
                    if (data.role === 'camera') {
                        cameraSocket = ws;
                        console.log('Camera registered');
                    } else if (data.role === 'controller') {
                        controllerSocket = ws;
                        console.log('Controller registered');
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
    if (recipient && recipient.readyState === 1) { // 1 = OPEN
        recipient.send(JSON.stringify(data));
    }
}

// 4. Start Server
server.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
