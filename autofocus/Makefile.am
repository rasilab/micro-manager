AUTOMAKE_OPTIONS = foreign subdir-objects
mmautofocus_DATA = \
	HardwareFocusExtender.jar \
	MMAutofocus.jar \
	MMAutofocusDuo.jar \
	MMAutofocusTB.jar \
	MMOughtaFocus.jar

.PHONY: autofocus.stamp

autofocus.stamp:
	@rm -f autofocus.tmp
	@touch autofocus.tmp
	$(ANT) -Dmm.javacflags="$(JAVACFLAGS)" $(ANTFLAGS) \
		-Dautofocus.intdir="`pwd`/build" -Dautofocus.outdir="`pwd`" jar
	@mv -f autofocus.tmp $@

$(mmautofocus_DATA): autofocus.stamp
	@if test -f $@; then :; else \
		rm -f autofocus.stamp; \
		$(MAKE) $(AM_MAKEFLAGS) autofocus.stamp; \
	fi

CLEANFILES = autofocus.stamp

clean-local:
	$(ANT) $(ANTFLAGS) -Dautofocus.intdir="`pwd`/build" -Dautofocus.outdir="`pwd`" clean
