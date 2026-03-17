import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { ConfigProvider } from './context/ConfigContext';
import { TemplateProvider } from './context/TemplateContext';
import Header from './components/Header';
import ConfigTable from './components/ConfigTable';
import ConfigForm from './components/ConfigForm';
import HtmlEditorPage from './components/HtmlEditorPage';
import './App.css';

function App() {
  return (
    <Router>
      <ConfigProvider>
        <TemplateProvider>
          <Header />
          <Routes>
            <Route path="/" element={<ConfigTable />} />
            <Route path="/config/new" element={<ConfigForm />} />
            <Route path="/config/:id" element={<ConfigForm />} />
            <Route path="/editor" element={<HtmlEditorPage />} />
          </Routes>
        </TemplateProvider>
      </ConfigProvider>
    </Router>
  );
}

export default App;
