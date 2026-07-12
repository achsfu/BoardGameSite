(function () {
    const activeRequests = new Map();
    const formSubmitHandlers = new WeakSet();

    function clearSuggestions(listElement) {
        listElement.innerHTML = "";
    }

    function fillSuggestions(listElement, suggestions) {
        clearSuggestions(listElement);

        suggestions.forEach(function (name) {
            const option = document.createElement("option");
            option.value = name;
            listElement.appendChild(option);
        });
    }

    function resolveGameName(term) {
        return fetch("/api/boardgames/resolve?q=" + encodeURIComponent(term))
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("Resolve request failed");
                }
                return response.json();
            })
            .then(function (data) {
                return data && typeof data.resolvedName === "string" ? data.resolvedName : "";
            })
            .catch(function () {
                return "";
            });
    }

    function bindResolveOnEnter(input) {
        const form = input.form;
        if (!form || formSubmitHandlers.has(form)) {
            return;
        }

        formSubmitHandlers.add(form);
        form.addEventListener("submit", function (event) {
            if (form.dataset.skipResolveSubmit === "true") {
                form.dataset.skipResolveSubmit = "false";
                return;
            }

            if (form.dataset.isResolvingSubmit === "true") {
                return;
            }

            const gameInputElements = Array.from(form.querySelectorAll("input[data-boardgame-input='true']"));
            if (gameInputElements.length === 0) {
                return;
            }

            event.preventDefault();
            form.dataset.isResolvingSubmit = "true";

            Promise.all(
                gameInputElements.map(function (field) {
                    const term = field.value.trim();
                    if (term.length === 0) {
                        return Promise.resolve();
                    }

                    return resolveGameName(term).then(function (resolvedName) {
                        if (resolvedName) {
                            field.value = resolvedName;
                        }
                    });
                })
            ).finally(function () {
                form.dataset.isResolvingSubmit = "false";
                form.dataset.skipResolveSubmit = "true";
                form.requestSubmit();
            });
        });
    }

    function attachGameAutocomplete(inputId, listId) {
        const input = document.getElementById(inputId);
        const list = document.getElementById(listId);

        if (!input || !list) {
            return;
        }

        input.dataset.boardgameInput = "true";
        bindResolveOnEnter(input);

        let debounceTimer = null;

        input.addEventListener("input", function () {
            const term = input.value.trim();

            if (debounceTimer) {
                clearTimeout(debounceTimer);
            }

            debounceTimer = setTimeout(function () {
                if (term.length < 1) {
                    clearSuggestions(list);
                    return;
                }

                const previousRequest = activeRequests.get(inputId);
                if (previousRequest) {
                    previousRequest.abort();
                }

                const controller = new AbortController();
                activeRequests.set(inputId, controller);

                fetch("/api/boardgames/autocomplete?q=" + encodeURIComponent(term), {
                    signal: controller.signal
                })
                    .then(function (response) {
                        if (!response.ok) {
                            throw new Error("Autocomplete request failed");
                        }
                        return response.json();
                    })
                    .then(function (suggestions) {
                        if (Array.isArray(suggestions)) {
                            fillSuggestions(list, suggestions);
                        } else {
                            clearSuggestions(list);
                        }
                    })
                    .catch(function (error) {
                        if (error.name !== "AbortError") {
                            clearSuggestions(list);
                        }
                    });
            }, 120);
        });

        input.addEventListener("keydown", function (event) {
            if (event.key !== "Enter") {
                return;
            }

            const form = input.form;
            if (!form) {
                return;
            }

            event.preventDefault();
            form.requestSubmit();
        });
    }

    window.attachGameAutocomplete = attachGameAutocomplete;
})();
