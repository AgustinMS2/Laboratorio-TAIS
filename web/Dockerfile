# Frontend estatico servido por nginx; tambien hace de proxy hacia las APIs (evita CORS)
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY index.html /usr/share/nginx/html/index.html
RUN chmod -R 755 /usr/share/nginx/html && chmod 644 /etc/nginx/conf.d/default.conf
EXPOSE 80
