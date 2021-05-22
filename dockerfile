FROM alpine
ADD * /app
RUN /usr/bin/crontab /app/crontab.txt
CMD ['crond', '-l 2', '-f']