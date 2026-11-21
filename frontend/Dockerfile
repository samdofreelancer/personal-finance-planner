# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
# `npm ci` installs the exact lockfile versions and is faster than `npm install`;
# the npm cache mount avoids re-downloading packages on every build.
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
RUN npm run build

# Run stage with Nginx
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
