SRC_ROOT=../src
DIST=../dist
UNI_JAR=u_universum.jar
SIGNED_JAR=universum.jar

#TAR=tar
TAR=/opt/local/bin/gnutar

JAVA_SRC=$(shell      \
           find       \
              $(SRC_ROOT)  \
             -name CVS -prune -o \
	     -name beings -prune -o \
             -name '*.java' -print \
           )

JAVA_CLASSES := $(patsubst $(SRC_ROOT)/%.java,$(DIST)/%.class,$(JAVA_SRC))

all: sanity prepare uni makebeing $(SIGNED_JAR)
	@echo Done

sanity:
	@if [ ! -d $(JAVA_HOME)/jre -a ! -f  $(JAVA_HOME)/bin/java ]; then \
           echo "Please set JAVA_HOME to 1.8 JDK, use forward slashes on Win32";\
           exit 1; \
        fi

prepare:
	@rm -rf changed
	@mkdir -p $(DIST)
	@if [ ! -d $(DIST)/resources/img ]; then \
	  cp -r ../resources/resources  $(DIST); \
         fi 

docs:
	$(A)mkdir -p ../docs
	$(A)$(JAVA_HOME)/bin/javadoc  -windowtitle "Electric Jungle SDK" -d ../docs -public -sourcepath ../src/ universum.bi


uni: $(UNI_JAR)

ifdef BEING
makebeing:  
	$(JAVA_HOME)/bin/java -jar $(UNI_JAR) -makebeing $(BEING)
else
makebeing:
endif

$(UNI_JAR): prepare classes
	@if  [ -f changed -o ! -f $@ ]; then \
          echo "Remaking $@.."; \
          rm -f $@ && \
          $(JAVA_HOME)/bin/jar -cmf MANIFEST.MF $@ -C $(DIST) . ; \
        fi

classes: $(JAVA_CLASSES)
	@if [ -f changed ]; then \
          printf "Compiling " && printf "`wc -l changed`" && echo " files"; \
          $(JAVA_HOME)/bin/javac -deprecation -source 1.8 -target 1.8 \
             -classpath $(DIST) -Xlint:unchecked \
            -d $(DIST) `cat changed`; \
        fi
	@-chmod -R 777 $(DIST)

$(DIST)/%.class: $(SRC_ROOT)/%.java
	@echo $< >> changed
	@echo "$<...."

run: $(SIGNED_JAR)
	$(JAVA_HOME)/bin/java -ea -jar $(UNI_JAR) -secure

run1: $(SIGNED_JAR)
	$(JAVA_HOME)/bin/java -Xmx150M -Xms100M -XX:+UseConcMarkSweepGC -cp $(DIST) universum.Main

all_beings:
	@for i in ../src/universum/beings/*.properties; do \
             $(JAVA_HOME)/bin/java -jar $(UNI_JAR) -makebeing $$i; \
        done

distrib: docs
	@echo "Making distr.jar..." 
	@rm -rf distr list
	@mkdir distr
	@mkdir distr/src
	@echo ../src/universum/engine/*.java >> list
	@echo ../src/universum/engine/topology/*.java >> list
	@echo ../src/universum/engine/resource/*.java >> list
	@echo ../src/universum/engine/things/*.java >> list
	@echo ../src/universum/ui/*.java >> list
	@echo ../src/universum/ui/sam/*.java >> list
	@echo ../src/universum/util/*.java >> list
	@echo ../src/universum/bi/*.java >> list
	@echo ../src/universum/Main.java >> list
	@echo ../src/universum/Tool.java >> list
	@echo ../src/universum/beings/SimpleBeing.java >> list
	@echo ../src/universum/beings/BeingTemplate.java >> list
	@$(JAVA_HOME)/bin/javac -source 1.8 -target 1.8 \
            -d distr -cp distr \
            `cat list` 
	@$(TAR) cf - `cat list` \
           ../bld/Makefile ../bld/MANIFEST.MF ../build.xml ../nbproject \
           ../bld/game.properties ../bld/single.properties ../bld/duel.properties \
	   ../bld/jungle.properties ../bld/alttopo.properties ../resources ../docs \
         | (cd distr && $(TAR) xf -)
	@cp ../README distr/README.txt
	@cp ../CREDITS distr/CREDITS.txt
	@cp ../src/java.policy distr/src/java.policy
	@cp ../src/universum/beings/template.properties distr/src/universum/beings/template.properties
	@cp ../src/universum/beings/simple.properties distr/src/universum/beings/simple.properties
	@cp ../src/universum/beings/simple.png distr/src/universum/beings/simple.png
	@find ./distr -name CVS |xargs rm -rf
	@$(JAVA_HOME)/bin/jar -cmf MANIFEST.MF distr.jar -C distr .
	@rm -rf distr
	@rm -f list

# to create
#  keytool -genkey -alias signFiles -keystore mystore -keypass 123456 -dname "cn=Nikolay Igotti" -storepass 123456 -validity 2000
# to export
# keytool -export -keystore mystore -storepass 123456 -alias signFiles -file Igotti.cer
# to import
# keytool -import -alias susan -file Igotti.cer -keystore raystore -storepass 111111
$(SIGNED_JAR):  $(UNI_JAR)
	@if [ -f mystore ]; then \
	 echo "Signing $(SIGNED_JAR).."; \
	 $(JAVA_HOME)/bin/jarsigner -keystore mystore \
	   -storepass 123456 -keypass 123456  \
	   -signedjar $(SIGNED_JAR) $(UNI_JAR) signFiles ;\
         else \
	  echo "Cannot make signed JAR, using unsigned one"; \
          cp $(UNI_JAR) $(SIGNED_JAR); \
         fi

clean:
	@rm -rf $(DIST)/* being ../docs
	@rm -f changed list MANIFEST_BEING.MF $(SIGNED_JAR) $(UNI_JAR) *.jar hotspot.log 
