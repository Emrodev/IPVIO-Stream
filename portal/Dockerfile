FROM node:20-alpine

WORKDIR /app

RUN apk add --no-cache python3 make g++

COPY package*.json ./
RUN npm install --omit=dev

COPY . .

EXPOSE 3000

VOLUME ["/app/data", "/app/public/apks"]

CMD ["node", "src/server.js"]
