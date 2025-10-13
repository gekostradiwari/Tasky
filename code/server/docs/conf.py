import os
import sys
from datetime import datetime

# -- Path setup --------------------------------------------------------------
PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
APP_PATH = os.path.join(PROJECT_ROOT)
if APP_PATH not in sys.path:
    sys.path.insert(0, APP_PATH)

# -- Project information -----------------------------------------------------
project = 'TaskyAPI'
author = 'TaskyAPI Developers'
copyright = f"{datetime.now():%Y}, {author}"
release = '0.1.0'

# -- General configuration ---------------------------------------------------
extensions = [
    'sphinx.ext.autodoc',
    'sphinx.ext.napoleon',
    'sphinx.ext.viewcode',
    'sphinx.ext.todo',
    'myst_parser',
    'sphinx_copybutton',
]

autodoc_default_options = {
    'members': True,
    'undoc-members': True,
    'show-inheritance': True,
    'inherited-members': False,
}

# Napoleon (Google/Numpy style) plus custom Italian sections mapping.
napoleon_custom_sections = [
    ('Descrizione', 'returns'),
    ('Input', 'params'),
    ('Output', 'returns'),
]

language = 'it'
source_suffix = {
    '.rst': 'restructuredtext',
    '.md': 'markdown',
}

exclude_patterns = ['_build', 'Thumbs.db', '.DS_Store']

templates_path = ['_templates']

# -- Options for HTML output -------------------------------------------------
html_theme = 'sphinx_rtd_theme'
html_static_path = ['_static']
html_logo = None
html_theme_options = {
    'collapse_navigation': False,
    'style_external_links': True,
}

todo_include_todos = True

# -- Intersphinx (optional placeholder) --------------------------------------
# intersphinx_mapping = { 'python': ('https://docs.python.org/3', {}) }

# -- Autodoc mock imports if DB libs absent ----------------------------------
autodoc_mock_imports = [
    'PyMySQL',
]

# Ensure consistent ordering
autodoc_member_order = 'bysource'

# MyST configuration
myst_enable_extensions = [
    'deflist',
    'colon_fence',
]
