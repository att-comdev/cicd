 ARG FROM=ubuntu:xenial
 FROM \\\${FROM}

 ENV PATH=/var/lib/openstack/bin:\\\$PATH

 COPY scripts /opt/loci/scripts
 ADD bindep.txt pydep.txt $EXTRA_BINDEP $EXTRA_PYDEP /opt/loci/
