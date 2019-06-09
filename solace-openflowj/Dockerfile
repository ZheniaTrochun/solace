FROM hseeberger/scala-sbt

ADD . /app

WORKDIR /app

RUN sbt compile

CMD ["sbt", "run"]

EXPOSE 6633