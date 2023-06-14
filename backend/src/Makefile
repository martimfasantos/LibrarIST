.PHONY: start clean

PYTHON := python3
MODELS := $(wildcard models/*.py)
SERVER := server.py
APP := app.py
SUDO := sudo

SUCCESS_COLOR := $(shell tput setaf 2)
ERROR_COLOR := $(shell tput setaf 1)
NO_COLOR := $(shell tput sgr0)

start:
	@for f in $(MODELS) $(SERVER) $(APP); do \
		if [ $$f = $(APP) ]; then \
			echo "$(SUCCESS_COLOR)$$f has started RUNNING!!$(NO_COLOR)"; \
		fi; \
		$(PYTHON) $$f; \
		if [ $$? -eq 0 ]; then \
			echo "$(SUCCESS_COLOR)$$f -- SUCCESS!$(NO_COLOR)"; \
		else \
			echo "$(ERROR_COLOR)$$f -- FAILED!$(NO_COLOR)"; \
			exit 1; \
		fi; \
	done

clean:
	rm -rf ./models/__pycache__/
	rm -rf __pycache__/
	rm -rf images/books/*
	rm -rf images/libraries/*
