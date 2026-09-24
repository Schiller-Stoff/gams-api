FROM eclipse-temurin:11-jre

WORKDIR /hs
RUN apt-get update && apt-get install -y wget tar dos2unix expect

# Download Handle.Net 9.3.2 and PostgreSQL JDBC Driver
RUN wget https://handle.net/maven/net/handle/handle/9.3.2/handle-9.3.2-distribution.tar.gz && \
    tar -xzf handle-9.3.2-distribution.tar.gz && \
    mv handle-9.3.2/* . && \
    wget -P lib/ https://jdbc.postgresql.org/download/postgresql-42.6.0.jar

RUN mkdir -p /hs/svr_1

# Automate setup targeting all prompts including server cert encryption
RUN cat << 'EOF' > setup.exp
set timeout -1
spawn bin/hdl-setup-server /hs/svr_1
expect {
    "server)?(y/n)" { send "y\r"; exp_continue }
    "IP address" { send "192.168.1.1\r"; exp_continue }
    "encrypt the server private key?(y/n)" { send "n\r"; exp_continue }
    "encrypt the admin private key?(y/n)" { send "n\r"; exp_continue }
    "encrypt your private key?(y/n)" { send "n\r"; exp_continue }
    "passphrase" { send "\r"; exp_continue }
    "organization" { send "GAMS Archive\r"; exp_continue }
    "email address" { send "admin@gams.local\r"; exp_continue }
    -re "\\\]\\s*:" { send "\r"; exp_continue }
    eof
}
EOF

RUN expect setup.exp

# Overwrite the generated config.dct with the SQL-backed config
COPY config.dct /hs/svr_1/config.dct
RUN dos2unix /hs/svr_1/config.dct

EXPOSE 8000
CMD ["/hs/bin/hdl-server", "/hs/svr_1"]