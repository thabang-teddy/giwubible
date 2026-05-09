import { Link } from 'react-router-dom'

export default function HomePage() {
  return (
    <div className="d-flex flex-column align-items-center justify-content-center min-vh-100 text-center px-3">
      <h1 className="display-4 fw-bold mb-2">Giwu Bible</h1>
      <p className="lead text-muted mb-4">
        Read the KJV side-by-side with any other translation.
      </p>
      <Link to="/read" className="btn btn-primary btn-lg px-5">
        Start Reading
      </Link>
    </div>
  )
}
